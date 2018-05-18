package telegram.bot.video.security.helper;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CriteriaQueryBuilder.
 *
 * @author Anton Astrouski
 */
@SuppressWarnings("unused")
public class CriteriaQueryBuilder {

    private final EntityManager em;

    public CriteriaQueryBuilder(EntityManager em) {
        this.em = em;
    }

    @FunctionalInterface
    public interface Where {
        void where(CriteriaBuilder b, CriteriaQuery q, Root r);
    }

    @FunctionalInterface
    public interface FIJoin {
        void join(Root r);
    }

    @FunctionalInterface
    public interface Predicate {
        javax.persistence.criteria.Predicate[] build(CriteriaBuilder b, Root r, Join j);
    }

    private <T, F> CriteriaQuery<T> prepareSingleColumnQuery(Class<T> klass, Class<F> from, String fieldName, Where w) {
        final CriteriaBuilder b = em.getCriteriaBuilder();
        final CriteriaQuery<T> q = b.createQuery(klass);
        Root<F> r = q.from(from);
        q.select(r.get(fieldName));
        w.where(b, q, r);
        return q;
    }

    private <T> CriteriaQuery<T> prepareQuery(Class<T> klass, Where w, FIJoin j) {
        final CriteriaBuilder b = em.getCriteriaBuilder();
        final CriteriaQuery<T> q = b.createQuery(klass);
        Root<T> r = q.from(klass);
        j.join(r);
        w.where(b, q, r);
        return q;
    }

    private <T> CriteriaQuery<Long> prepareIdsQuery(Class<T> klass, Where w, FIJoin j) {
        final CriteriaBuilder b = em.getCriteriaBuilder();
        final CriteriaQuery<Long> q = b.createQuery(Long.class);
        Root<T> r = q.from(klass);
        j.join(r);
        w.where(b, q, r);
        return q;
    }

    private <T> CriteriaQuery<Long> prepareCountQuery(Class<T> klass, Where w) {
        final CriteriaBuilder b = em.getCriteriaBuilder();
        final CriteriaQuery<Long> q = b.createQuery(Long.class);
        Root<T> r = q.from(klass);
        q.select(b.count(r));
        w.where(b, q, r);
        return q;
    }

    /**
     * throws {@link javax.persistence.NonUniqueResultException}, {@link NoResultException}
     */
    public <T> T getReference(Class<T> klass, Where w, FIJoin j) {
        return em.createQuery(prepareQuery(klass, w, j)).getSingleResult();
    }

    /**
     * Returns found entity or null.
     * <p>throws {@link javax.persistence.NonUniqueResultException}</p>
     */
    public <T> T find(Class<T> klass, Where w, FIJoin j) {
        try {
            return em.createQuery(prepareQuery(klass, w, j)).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Returns found entity or null.
     * <p>throws {@link javax.persistence.NonUniqueResultException}</p>
     */
    public <T> T findFirst(Class<T> klass, Where w, FIJoin j) {
        try {
            return em.createQuery(prepareQuery(klass, w, j)).setMaxResults(1).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Returns count result.
     */
    public <T> Long getCount(Class<T> klass, Where w) {
        return em.createQuery(prepareCountQuery(klass, w)).getSingleResult();
    }

    /**
     * Returns entity list.
     * <p>Specify join/fetch in where section if needed</p>
     */
    public <T> List<T> getList(Class<T> klass, Where w) {
        return em.createQuery(prepareQuery(klass, w, r -> {
        })).getResultList();
    }

    public <T, F> Set<T> getSingleColumnList(Class<T> klass, Class<F> from, String fieldName, Where w) {
        return new HashSet<>(em.createQuery(prepareSingleColumnQuery(klass, from, fieldName, w)).getResultList());
    }

    public <T, F> Set<T> getSingleColumnList(Class<T> klass, Class<F> from, String fieldName) {
        return getSingleColumnList(klass, from, fieldName, (b, q, r) -> {
        });
    }

    /**
     * Returns id list.
     * <p>Specify join/fetch in where section if needed</p>
     */
    public <T> List<Long> getIdList(Class<T> klass, Where w) {
        return em.createQuery(prepareIdsQuery(klass, w, r -> {
        })).getResultList();
    }

    /**
     * Returns entity list for paging load.
     * <p>Specify join/fetch in where section if needed</p>
     */
    public <T> List<T> getList(Class<T> klass, Where w, int firstResult, int maxResults) {
        return em.createQuery(prepareQuery(klass, w, r -> {
        })).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }
}
