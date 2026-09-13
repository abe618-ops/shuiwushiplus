package com.abe618.liurenfootball;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class Models {
    private Models() {}

    static final class MatchItem {
        String id = "";
        String league = "";
        String date = "";
        String time = "";
        String home = "主队";
        String away = "客队";
        String homeTeamId = "";
        String awayTeamId = "";
        Integer homeFoundedYear;
        Integer awayFoundedYear;
    }

    static final class TeamMeta {
        String teamId = "";
        String name = "";
        Integer foundedYear;
    }

    static final class InputMeta {
        String homeName = "主队";
        String awayName = "客队";
        Integer homeFoundedYear;
        Integer awayFoundedYear;
        Integer homeCoachBirthYear;
        Integer awayCoachBirthYear;
    }

    static final class ChartSeed {
        String modeName = "";
        int poolIndex = -1;
        LocalDate date;
        int dayIndex;
        int hourBranch;
        int monthGeneral;
        long frozenAtMillis;

        String token() {
            return modeName + "|" + poolIndex + "|" + date + "|" + dayIndex + "|" + hourBranch + "|" + monthGeneral + "|" + frozenAtMillis;
        }
    }

    static final class Course {
        int index;
        int lowerBranch;
        Integer lowerStem;
        int upperBranch;
        String lowerLabel;

        Course(int index, int lowerBranch, Integer lowerStem, int upperBranch, String lowerLabel) {
            this.index = index;
            this.lowerBranch = lowerBranch;
            this.lowerStem = lowerStem;
            this.upperBranch = upperBranch;
            this.lowerLabel = lowerLabel;
        }
    }

    static final class Chart {
        ChartSeed seed;
        int dayStem;
        int dayBranch;
        int[] skyAt = new int[12];
        int[] generalOnSky = new int[12];
        Course[] courses = new Course[4];
        int initial;
        int middle;
        int finalTransmission;
        String pattern = "";
        int monthBuild;
        int nobleBranch;
        boolean nobleForward;
        String notes = "";
    }

    static final class LocationVote {
        String source;
        double weight;
        String vote;
        String detail;

        LocationVote(String source, double weight, String vote, String detail) {
            this.source = source;
            this.weight = weight;
            this.vote = vote;
            this.detail = detail;
        }
    }

    static final class LocationResult {
        boolean homeIsBranch = true;
        double branchVotes;
        double stemVotes;
        double consistency = 1.0;
        List<LocationVote> votes = new ArrayList<>();
    }

    static final class ScoreLine {
        String item;
        String detail;
        double homeDelta;
        double drawDelta;
        double awayDelta;

        ScoreLine(String item, String detail, double homeDelta, double drawDelta, double awayDelta) {
            this.item = item;
            this.detail = detail;
            this.homeDelta = homeDelta;
            this.drawDelta = drawDelta;
            this.awayDelta = awayDelta;
        }
    }

    static final class PredictionResult {
        String mode;
        Chart chart;
        LocationResult location;
        double homeScore;
        double drawScore;
        double awayScore;
        double pHome;
        double pDraw;
        double pAway;
        String verdict;
        String extendedSummary;
        List<ScoreLine> scoreLines = new ArrayList<>();
    }
}
