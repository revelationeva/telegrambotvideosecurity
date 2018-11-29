package telegram.bot.video.security.core;

import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.processing.face.detection.DatasetFaceDetector;
import org.openimaj.image.processing.face.detection.keypoints.KEDetectedFace;
import telegram.bot.video.security.entity.Face;
import telegram.bot.video.security.entity.Person;
import telegram.bot.video.security.face.FaceRecognizer;
import telegram.bot.video.security.helper.CriteriaQueryBuilder;

import javax.persistence.EntityManager;
import javax.persistence.criteria.JoinType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersonManager {

    private SingleContext ctx;
    private EntityManager localEm;

    public PersonManager() {
        ctx = SingleContext.getInstance();
    }

    public void init() {
        localEm = ctx.borrowEm(true);
        try {
            String name = "Anton";
            if (countPersonNames(name) == 0) {
                Person p = persistNewPerson(name);
                persistFaceForPerson(p, "Anton.jpg");
                persistFaceForPerson(p, "Anton2.jpg");
                persistFaceForPerson(p, "Anton3.png");
            }
            name = "Aliaksei";
            if (countPersonNames(name) == 0) {
                Person p = persistNewPerson(name);
                persistFaceForPerson(p, "Aliaksei.jpg");
            }
            name = "Aliaksandra";
            if (countPersonNames(name) == 0) {
                Person p = persistNewPerson(name);
                persistFaceForPerson(p, "Aliaksandra.jpg");
            }
            ctx.returnEm(localEm, true);
        } catch (IOException e) {
            localEm.getTransaction().rollback();
            e.printStackTrace();
        }
    }

    private Person persistNewPerson(String name) {
        Person p = new Person(name, "");
        localEm.persist(p);
        return p;
    }

    private void persistFaceForPerson(Person persistentPerson, String path) throws IOException {
        Face f = new Face(persistentPerson, Files.readAllBytes(Paths.get(path)));
        localEm.persist(f);
    }

    private long countPersonNames(String name) {
        return new CriteriaQueryBuilder(ctx.getEm()).getCount(Person.class, (b, q, r) -> q.where(b.equal(r.get("firstName"), name)));
    }

    public List<Person> getAll(boolean fetchFaces) {
        return new CriteriaQueryBuilder(ctx.getEm()).getList(Person.class, (b, q, r) -> {
            if (fetchFaces) {
                q.select(r).distinct(true);
                r.fetch("faces", JoinType.LEFT);
            }
        });
    }

    public Map<KEDetectedFace, Person> getFaces() {
        Map<KEDetectedFace, Person> faces = new HashMap<>();
        List<Person> persons = getAll(true);
        persons.forEach(person -> person.getFaces().forEach(face -> {
            try {
                FImage fImage = ImageUtilities.readF(new ByteArrayInputStream(face.getFace()));
                KEDetectedFace biggest = DatasetFaceDetector.getBiggest(FaceRecognizer.faceDetector
                        .detectFaces(fImage));
                faces.put(biggest, person);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        return faces;
    }

    public void add(List<Person> persons) {
        persons.forEach(person -> ctx.persist(person, true));
    }
}