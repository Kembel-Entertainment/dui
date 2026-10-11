package gg.kembel.dui.paper;

import gg.kembel.dui.runtime.DialogResponse;
import io.papermc.paper.dialog.DialogResponseView;

public record PaperResponse(DialogResponseView nativeResponse) implements DialogResponse {
  public String text(String key) { return nativeResponse == null ? null : nativeResponse.getText(key); }
  public Float number(String key) { return nativeResponse == null ? null : nativeResponse.getFloat(key); }
  public Boolean bool(String key) { return nativeResponse == null ? null : nativeResponse.getBoolean(key); }
}
