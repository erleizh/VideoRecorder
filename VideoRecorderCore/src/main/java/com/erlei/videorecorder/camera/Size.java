package com.erlei.videorecorder.camera;


public class Size {
    protected int width;
    protected int height;

    public Size(android.hardware.Camera.Size size) {
        if (size == null) return;
        width = size.width;
        height = size.height;
    }

    public Size(Size size) {
        if (size == null) return;
        width = size.width;
        height = size.height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Size(int w, int h) {
        width = w;
        height = h;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Size size = (Size) o;
        return width == size.width && height == size.height;
    }

    boolean equals(android.hardware.Camera.Size size) {
        return size != null && width == size.width && height == size.height;
    }

    public String toLongString() {
        return "Size{" +
                "width=" + width +
                ", height=" + height +
                '}';

    }

    @Override
    public String toString() {
        return width + "x" + height;
    }

    @Override
    public int hashCode() {
        int result = width;
        result = 31 * result + height;
        return result;
    }

    public static Size parseSize(String string) throws NumberFormatException {
        if (string == null) {
            throw new NullPointerException("string must not be null");
        }

        int sep_ix = string.indexOf('*');
        if (sep_ix < 0) {
            sep_ix = string.indexOf('x');
        }
        if (sep_ix < 0) {
            throw new IllegalArgumentException(string);
        }
        try {
            return new Size(Integer.parseInt(string.substring(0, sep_ix)),
                    Integer.parseInt(string.substring(sep_ix + 1)));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(string);
        }
    }
}
