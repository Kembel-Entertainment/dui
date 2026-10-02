package gg.kembel.dui.core;

/** Affine transformation shared by raster placement and pointer geometry. */
public record Transform2D(double a, double b, double c, double d, double tx, double ty) {
  public static final Transform2D IDENTITY = new Transform2D(1, 0, 0, 1, 0, 0);

  public Transform2D {
    for (double v : new double[] {a, b, c, d, tx, ty})
      if (!Double.isFinite(v)) throw new IllegalArgumentException("Nonfinite transform");
    if (Math.abs(a * d - b * c) < 1e-10) throw new IllegalArgumentException("Singular transform");
  }

  public record Point(double x, double y) {}

  public Point apply(double x, double y) {
    return new Point(a * x + c * y + tx, b * x + d * y + ty);
  }

  /** Parent.multiply(child) applies the child first. */
  public Transform2D multiply(Transform2D v) {
    return new Transform2D(
        a * v.a + c * v.b,
        b * v.a + d * v.b,
        a * v.c + c * v.d,
        b * v.c + d * v.d,
        a * v.tx + c * v.ty + tx,
        b * v.tx + d * v.ty + ty);
  }

  public Transform2D inverse() {
    double det = a * d - b * c;
    return new Transform2D(
        d / det, -b / det, -c / det, a / det, (c * ty - d * tx) / det, (b * tx - a * ty) / det);
  }

  public static Transform2D translation(double x, double y) {
    return new Transform2D(1, 0, 0, 1, x, y);
  }

  public static Transform2D scale(double x, double y) {
    return new Transform2D(x, 0, 0, y, 0, 0);
  }

  public static Transform2D rotation(double degrees) {
    double r = Math.toRadians(degrees);
    return new Transform2D(Math.cos(r), Math.sin(r), -Math.sin(r), Math.cos(r), 0, 0);
  }
}
