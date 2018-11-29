package telegram.bot.video.security.face;

import gnu.trove.list.array.TIntArrayList;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.math.combinatorics.optimisation.HungarianAlgorithm;
import org.openimaj.math.geometry.shape.Polygon;
import telegram.bot.video.security.core.camera.FaceRecognition;

import java.util.ArrayList;
import java.util.List;

public class Matcher extends FaceRecognizer {

    public static final String NO_MATCH = "No matches!";
    public static final String SIMILAR = "Face is similar with ";

    @Override
    public String detectFaces(List<? extends DetectedFace> groundTruth, List<? extends DetectedFace> detected) {
        List<Match> matches = match(groundTruth, detected);
        if (matches == null || matches.isEmpty()) {
            return NO_MATCH;
        }
        matches.sort((o1, o2) -> (int) (o1.score > o2.score ? o1.score : o2.score));
        Match m = matches.get(0);
        String res = FaceRecognition.facesCache.get(m.groundTruth).getFirstName() + " score:" + m.score;
        if (m.score < 0.04) {
            return SIMILAR + res;
        }
        return res;
    }

    public static class Match {
        DetectedFace groundTruth;
        DetectedFace detected;
        double score;

        Match(DetectedFace groundTruth, DetectedFace detected, double score) {
            this.groundTruth = groundTruth;
            this.detected = detected;
            this.score = score;
        }

        @Override
        public String toString() {
            return String.format("%s->%s : %f", groundTruth, detected, score);
        }
    }

    private double[][] computePairwiseScores(
            List<? extends DetectedFace> groundTruth,
            List<? extends DetectedFace> detected) {
        final int numGround = groundTruth.size();
        final int numDet = detected.size();
        final double[][] scores = new double[numGround][numDet];

        for (int i = 0; i < numGround; i++) {
            for (int j = 0; j < numDet; j++) {
                scores[i][j] = computeScore(groundTruth.get(i), detected.get(j));
            }
        }

        return scores;
    }

    private double computeScore(DetectedFace ground, DetectedFace detected) {
        final Polygon gs = ground.getShape().asPolygon();
        final Polygon ds = detected.getShape().asPolygon();

        return gs.intersect(ds).calculateArea() / gs.union(ds).calculateArea();
    }

    private List<Match> match(List<? extends DetectedFace> groundTruth, List<? extends DetectedFace> detected) {
        final double[][] scores = computePairwiseScores(groundTruth, detected);
        final double[][] filtered = collapse(scores);

        if (filtered.length == 0 || filtered[0].length == 0)
            return null;

        final HungarianAlgorithm ha = new HungarianAlgorithm(filtered);
        final int[] assignments = ha.execute();

        final List<Match> results = new ArrayList<>();
        for (int gtIdx = 0; gtIdx < assignments.length; gtIdx++) {
            if (assignments[gtIdx] >= 0) {
                final int detIdx = assignments[gtIdx];
                final double score = scores[gtIdx][detIdx];

                if (score > 0) {
                    results.add(new Match(groundTruth.get(gtIdx), detected.get(detIdx), score));
                }
            }
        }
        return results;
    }

    /**
     * Remove any rows or cols that sum to zero.
     */
    private double[][] collapse(double[][] scores) {
        // rows:
        final TIntArrayList rowsToKeep = new TIntArrayList(scores[0].length);
        for (int r = 0; r < scores.length; r++) {
            double sum = 0;
            for (int c = 0; c < scores[0].length; c++) {
                sum += scores[r][c];
            }
            if (sum != 0)
                rowsToKeep.add(r);
        }

        // cols:
        final TIntArrayList colsToKeep = new TIntArrayList(scores.length);
        for (int c = 0; c < scores[0].length; c++) {
            double sum = 0;
            for (double[] score : scores) {
                sum += score[c];
            }
            if (sum != 0)
                colsToKeep.add(c);
        }

        if (rowsToKeep.size() == scores[0].length && colsToKeep.size() == scores.length)
            return scores;

        final double[][] filtered = new double[rowsToKeep.size()][colsToKeep.size()];

        for (int r = 0; r < filtered.length; r++) {
            for (int c = 0; c < filtered[0].length; c++) {
                filtered[r][c] = scores[rowsToKeep.get(r)][colsToKeep.get(c)];
            }
        }

        return filtered;
    }
}