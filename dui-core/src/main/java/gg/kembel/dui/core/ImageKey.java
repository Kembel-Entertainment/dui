package gg.kembel.dui.core;

/** Includes every policy that affects a fitted image. */
public record ImageKey(
    String identity,
    String version,
    int width,
    int height,
    RgbaImage.Fit fit,
    RgbaImage.Sampling sampling,
    int background) {
  public ImageKey {
    if (identity == null || identity.isBlank() || version == null || width < 1 || height < 1)
      throw new IllegalArgumentException("Image cache key");
    java.util.Objects.requireNonNull(fit);
    java.util.Objects.requireNonNull(sampling);
  }
}
