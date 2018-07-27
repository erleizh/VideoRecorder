package com.erlei.videorecorder.camera;

public class FpsRange {

    public int min;
    public int max;
    public String str;

    public FpsRange(int[] ints) {
        min = ints[0];
        max = ints[1];
        str = min + "," + max;
    }

    public FpsRange(int min, int max) {
        this.min = min;
        this.max = max;
        str = min + "," + max;
    }

    public FpsRange(String s) {
        str = s.replaceAll("\\(", "").replaceAll("\\)", "");
        String[] split = str.split(",");
        try {
            min = Integer.parseInt(split[0]);
            max = Integer.parseInt(split[1]);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FpsRange fpsRange = (FpsRange) o;

        return min == fpsRange.min && max == fpsRange.max;
    }

    @Override
    public String toString() {
        return min + "," + max;
    }

    @Override
    public int hashCode() {
        int result = min;
        result = 31 * result + max;
        return result;
    }
}
