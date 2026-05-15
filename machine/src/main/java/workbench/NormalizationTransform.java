package workbench;

public record NormalizationTransform(double offsetX, double offsetY, double scale) {
    public NormalizedPoint apply(double px, double py) {
        return new NormalizedPoint((px - offsetX) / scale, (py - offsetY) / scale);
    }
}
