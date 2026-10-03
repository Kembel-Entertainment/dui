package gg.kembel.dui.core.video;

import gg.kembel.dui.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapVideoTest {
  private static VideoSurfaceSpec spec(int w,int h,PixelFormat f) {
    return new VideoSurfaceSpec(w,h,f,60,VideoSurfaceSpec.Viewport.FULL,new VideoSurfaceSpec.Budget(512,32*1024*1024),0x123456,false);
  }
  @Test void everyOriginal555ColorAndRgbSamplesSurviveTransport() {
    for(var format:PixelFormat.values()) {
      int[] pixels=new int[32768];var random=new Random(7);
      for(int i=0;i<pixels.length;i++)pixels[i]=format==PixelFormat.BGR555?i:random.nextInt(1<<24);
      var frame=new VideoFrame(256,128,format,1,pixels);
      var tiles=MapVideoCodec.encode(spec(256,128,format),frame);
      int seen=0;
      for(var tile:tiles) {
        byte[] colors=tile.colors();assertEquals(MapVideoCodec.VERSION,MapVideoCodec.get(colors,4,1));
        assertEquals(0x123456,MapVideoCodec.get(colors,27,4));
        for(int y=0;y<tile.height();y++)for(int x=0;x<tile.width();x++) {
          assertEquals(frame.pixel(tile.x()+x,tile.y()+y),MapVideoCodec.get(colors,(y+1)*128+x*format.symbols,format.symbols));seen++;
        }
      }
      assertEquals(pixels.length,seen);
    }
  }
  @Test void nativeCarrierColorsAreDistinctAndUpdatesApplyAtEdges() {
    var colors=new HashSet<Integer>();for(int i=0;i<192;i++)assertTrue(colors.add(MapVideoPalette.rgb(i+4)));
    byte[] before=new byte[128*128],after=before.clone();
    after[127]=3;after[after.length-128]=7;
    var patch=MapVideoCodec.difference(before,after);
    for(int y=0;y<patch.height();y++)System.arraycopy(patch.colors(),y*patch.width(),before,(y+patch.y())*128+patch.x(),patch.width());
    assertArrayEquals(after,before);assertNull(MapVideoCodec.difference(before,after));
    assertEquals(128*128,MapVideoCodec.difference(null,after).colors().length);
  }
  @Test void backpressureNeverRestoresOldFrameOverANewerOne() {
    var mailbox=new LatestFrame();var old=new VideoFrame(1,1,PixelFormat.RGB888,1,new int[]{1});
    var next=new VideoFrame(1,1,PixelFormat.RGB888,2,new int[]{2});
    assertTrue(mailbox.submit(old));assertSame(old,mailbox.poll());assertTrue(mailbox.submit(next));mailbox.restore(old);
    assertSame(next,mailbox.poll());mailbox.close();assertFalse(mailbox.submit(next));assertNull(mailbox.poll());
  }
  @Test void viewportIsTemplateOwnedAndBudgetsRejectOversizedSources() {
    var template=VideoSurfaceTemplate.parse("<dui-video width=\"{{w}}\" height=\"{{h}}\" format=\"RGB888\" fps=\"60\" left=\"0.1\" right=\"0.9\" max-tiles=\"17\" bytes-per-second=\"16777216\"/>",
        TestEnvironment.environment(),ComponentRegistry.EMPTY,"test");
    var rendered=template.render(Map.of("w",240,"h",160));assertEquals(.1,rendered.specification().viewport().left());
    assertThrows(IllegalArgumentException.class,()->template.render(Map.of("w",1000,"h",1000)));
    assertThrows(IllegalArgumentException.class,()->template.render(Map.of()));
    assertThrows(IllegalArgumentException.class,()->new VideoSurfaceSpec(240,160,PixelFormat.RGB888,60,VideoSurfaceSpec.Viewport.FULL,new VideoSurfaceSpec.Budget(2,1_000_000),0,false));
  }
}
