package telegram.bot.video.security.core;

import org.openimaj.video.capture.Device;
import telegram.bot.video.security.entity.Capture;
import telegram.bot.video.security.entity.IEntity;
import telegram.bot.video.security.helper.CriteriaQueryBuilder;
import telegram.bot.video.security.helper.Pair;
import telegram.bot.video.security.helper.UUIDUtil;
import telegram.bot.video.security.option.ControlOptions;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import static telegram.bot.video.security.TelegramBot.CONTROL_COMMANDS;

final class SingleContext {

    private static final Logger LOG = Logger.getLogger(SingleContext.class.getCanonicalName());

    private static volatile SingleContext instance;
    private EntityManagerFactory emf;
    private EntityManager em;

    private SingleContext() {
        /*emf = Persistence.createEntityManagerFactory("telegramBotDBCreatePU");
        emf.close();*/

        emf = Persistence.createEntityManagerFactory("telegramBotPU");
        em = emf.createEntityManager();
        final Set<String> usedUids = new CriteriaQueryBuilder(em).getSingleColumnList(String.class, Capture.class, "uid");
        for (Device d : CameraProcessor.getAllCameras()) {
            String uid;
            do {
                uid = UUIDUtil.getUpperUID6();
            } while (usedUids.contains(uid));
            availableCameras.put(uid, d);
        }

        final int poolSize = 4;
        for (int i = 0; i < poolSize; i++) {
            emPool.add(emf.createEntityManager());
        }
    }

    static SingleContext getInstance() {
        if (instance == null) {
            synchronized (SingleContext.class) {
                if (instance == null) {
                    instance = new SingleContext();
                }
            }
        }
        return instance;
    }

    private Vector<EntityManager> emPool = new Vector<>();
    private Vector<EntityManager> borrowedEms = new Vector<>();

    void persist(IEntity e, boolean useTx) {
        final EntityManager em = borrowEm(useTx);
        if (!em.contains(e) && e.getId() != null) {
            em.merge(e);
        } else {
            em.persist(e);
        }
        returnEm(em, useTx);
    }

    public EntityManager borrowEm(boolean startTx) {
        while (emPool.size() == 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                LOG.log(Level.SEVERE, "Exception while awaiting connection!", e);
            }
        }
        final EntityManager em = emPool.remove(0);
        borrowedEms.add(em);
        if (startTx) {
            em.getTransaction().begin();
        }
        return em;
    }

    public void returnEm(EntityManager em, boolean commitActiveTx) {
        if (em != null) {
            final EntityTransaction tx = em.getTransaction();
            if (tx.isActive() && commitActiveTx) {
                tx.commit();
            }
        }
        if (borrowedEms.remove(em)) {
            emPool.add(em);
        } else {
            LOG.warning("No such borrowed em found! em: " + em);
        }
    }

    EntityManager getEm() {
        return em;
    }

    Pair<String, Device> current;
    Map<String, Device> availableCameras = new HashMap<>();
    Map<String, Device> busyCameras = new HashMap<>();

    void freeCam(String camUid) {
        swap(camUid, busyCameras, availableCameras);
    }

    private void swap(String camUid, Map<String, Device> from, Map<String, Device> to) {
        Device remove = from.remove(camUid);
        if (remove != null) {
            to.put(camUid, remove);
        }
    }

    String useCam(ControlOptions co) {
        Device avail = availableCameras.remove(co.uid);
        if (avail != null) {
            busyCameras.put(co.uid, avail);
            current = new Pair<>(co.uid, avail);
            return null;
        } else {
            Device busy = busyCameras.get(co.uid);
            if (busy != null) {
                return "This camera (" + busy.getNameStr() + ") is used for now. To free it type the following command:\n" +
                        co.uid + " " + CONTROL_COMMANDS[0];
            } else {
                return "No camera with such name exists.";
            }
        }
    }
}
