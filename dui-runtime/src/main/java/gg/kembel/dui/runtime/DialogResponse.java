package gg.kembel.dui.runtime;

/** Typed dialog values normalized at the platform boundary. Missing values return null. */
public interface DialogResponse {
  DialogResponse EMPTY = new DialogResponse() {
    public String text(String key) { return null; }
    public Float number(String key) { return null; }
    public Boolean bool(String key) { return null; }
  };
  String text(String key);
  Float number(String key);
  Boolean bool(String key);
}
