package gg.kembel.dui.core;

/** Cheap layout diagnostics. These are counts, not measured network bytes or GPU timings. */
public record RenderReport(
    int width,
    int height,
    int paints,
    int hits,
    int heads,
    int nativeItems,
    int shaderEffects,
    int imagePixels,
    int remainingEffects,
    int remainingImagePixels,
    int playerModels) {
  public RenderReport(
      int width,
      int height,
      int paints,
      int hits,
      int heads,
      int nativeItems,
      int shaderEffects,
      int imagePixels,
      int remainingEffects,
      int remainingImagePixels) {
    this(
        width,
        height,
        paints,
        hits,
        heads,
        nativeItems,
        shaderEffects,
        imagePixels,
        remainingEffects,
        remainingImagePixels,
        0);
  }

  public static final int IMAGE_PIXEL_LIMIT = 16_384;

  public static RenderReport of(Canvas canvas) {
    int pixels = canvas.images.stream().mapToInt(i -> i.raster().width * i.raster().height).sum();
    return new RenderReport(
        canvas.width,
        canvas.height,
        canvas.paints.size(),
        canvas.hits.size(),
        canvas.heads.size(),
        canvas.items.size(),
        canvas.effects.size(),
        pixels,
        canvas.effectLimit - canvas.effects.size(),
        IMAGE_PIXEL_LIMIT - pixels,
        canvas.playerModels.size());
  }
}
