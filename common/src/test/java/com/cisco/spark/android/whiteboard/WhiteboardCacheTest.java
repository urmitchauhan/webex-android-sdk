package com.cisco.spark.android.whiteboard;

import android.graphics.Color;

import com.cisco.spark.android.model.Json;
import com.cisco.spark.android.whiteboard.persistence.model.Whiteboard;
import com.cisco.spark.android.whiteboard.util.WhiteboardUtils;
import com.cisco.spark.android.whiteboard.view.event.WhiteboardRealtimeEvent;
import com.cisco.spark.android.whiteboard.view.model.Stroke;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wacom.ink.rasterization.BlendMode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class WhiteboardCacheTest {
    public static final int STRIDE = 3;

    @Mock EventBus bus;
    Gson gson;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        gson = Json.buildGson();
    }

    @Test
    public void testCacheMiss() {
        String boardId = "3a43ce96-ef5d-496b-9492-88a99b40adbe";
        WhiteboardCache whiteboardCache = new WhiteboardCache(bus, gson);
        assertNull(whiteboardCache.getWhiteboard(boardId));
    }

    @Test
    public void testSimpleCacheHit() {
        String boardId = "58ccff00-8661-4b6f-8980-10737af51792";
        List<Stroke> strokeList = new ArrayList<>();
        strokeList.add(mock(Stroke.class));
        strokeList.add(mock(Stroke.class));
        strokeList.add(mock(Stroke.class));
        WhiteboardCache whiteboardCache = new WhiteboardCache(bus, gson);
        whiteboardCache.initAndStartRealtimeForBoard(boardId, strokeList);

        Whiteboard cachedWhiteboard = whiteboardCache.getWhiteboard(boardId);
        assertNotNull(cachedWhiteboard);
        assertEquals(boardId, cachedWhiteboard.getId());
        List<Stroke> cachedStrokeList = cachedWhiteboard.getStrokes();
        assertEquals("The original strokes and the cached strokes should be identical (number of strokes differs)", strokeList.size(), cachedStrokeList.size());
        for (int i = 0; i < strokeList.size(); i++) {
            assertEquals("The original strokes and the cached strokes should be identical (stroke " + i + " is different)", strokeList.get(i), cachedStrokeList.get(i));
        }
    }

    @Test
    public void testAddStrokeToCurrentRealtimeBoard() {
        String boardId = "aab69ef4-8b89-44ea-adc6-a1305b675230";
        List<Stroke> strokeList = new ArrayList<>();
        strokeList.add(new Stroke(new float[] { 90.0f, 111.0f, 1.0f, 1501.0f, 989.0f, 1.0f }, Color.BLUE, STRIDE, BlendMode.BLENDMODE_NORMAL));
        WhiteboardCache whiteboardCache = new WhiteboardCache(bus, gson);
        whiteboardCache.initAndStartRealtimeForBoard(boardId, strokeList);

        whiteboardCache.addStrokeToCurrentRealtimeBoard(new Stroke(new float[] { 1337.0f, 123.0f, 1.0f, 311.0f, 321.0f, 1.0f }, Color.RED, STRIDE, BlendMode.BLENDMODE_NORMAL));
        whiteboardCache.addStrokeToCurrentRealtimeBoard(new Stroke(new float[] { 1337.0f, 123.0f, 1.0f, 311.0f, 321.0f, 1.0f }, Color.BLACK, STRIDE, BlendMode.BLENDMODE_ERASE));
        Whiteboard cachedWhiteboard = whiteboardCache.getWhiteboard(boardId);
        assertEquals(3, cachedWhiteboard.getStrokes().size());
        assertEquals(BlendMode.BLENDMODE_NORMAL, cachedWhiteboard.getStrokes().get(1).getBlendMode());
        assertEquals(Color.RED, cachedWhiteboard.getStrokes().get(1).getColor());
        assertEquals(BlendMode.BLENDMODE_ERASE, cachedWhiteboard.getStrokes().get(2).getBlendMode());
    }

    @Test
    public void testClearCurrentRealtimeBoard() {
        String boardId = "4ab46bcd-ac5f-4f17-9901-26136d3365e5";
        List<Stroke> strokeList = new ArrayList<>();
        strokeList.add(mock(Stroke.class));
        strokeList.add(mock(Stroke.class));
        strokeList.add(mock(Stroke.class));
        strokeList.add(mock(Stroke.class));
        strokeList.add(mock(Stroke.class));
        strokeList.add(mock(Stroke.class));
        WhiteboardCache whiteboardCache = new WhiteboardCache(bus, gson);
        whiteboardCache.initAndStartRealtimeForBoard(boardId, strokeList);

        whiteboardCache.clearCurrentRealtimeBoard();
        assertEquals(0, whiteboardCache.getWhiteboard(boardId).getStrokes().size());
    }

    @Test
    public void testEvictAll() {
        String boardId = "6f5f9fd4-f11e-4e84-a004-c25b7a38a1a0";
        List<Stroke> strokeList = new ArrayList<>();
        strokeList.add(mock(Stroke.class));
        strokeList.add(mock(Stroke.class));
        WhiteboardCache whiteboardCache = new WhiteboardCache(bus, gson);
        whiteboardCache.initAndStartRealtimeForBoard(boardId, strokeList);

        whiteboardCache.evictAll();
        assertNull(whiteboardCache.getWhiteboard(boardId));
    }

    @Test
    public void testIncomingRealtimeStrokesShouldBeAppendedToCache() {
        String boardId = "b6cd01d8-d2ed-41de-af0d-f387f266bd36";
        List<Stroke> strokeList = new ArrayList<>();
        strokeList.add(new Stroke(new float[] { 11.0f, 200.0f, 1.0f, 50.0f, 30.0f, 1.0f }, Color.RED, STRIDE, BlendMode.BLENDMODE_NORMAL));
        strokeList.add(new Stroke(new float[] { 33.0f, 11.0f, 1.0f, 900.0f, 1000.0f, 1.0f }, Color.BLUE, STRIDE, BlendMode.BLENDMODE_NORMAL));
        WhiteboardCache whiteboardCache = new WhiteboardCache(bus, gson);
        whiteboardCache.initAndStartRealtimeForBoard(boardId, strokeList);

        whiteboardCache.onEventBackgroundThread(createContentCommitJson(new float[] { 120.0f, 90.0f, 1.0f, 10.0f, 5.0f, 1.0f }, Color.BLACK, BlendMode.BLENDMODE_ERASE));
        whiteboardCache.onEventBackgroundThread(createContentCommitJson(new float[] { 900.0f, 50.0f, 1.0f, 1100.0f, 30.0f, 1.0f }, Color.GREEN, BlendMode.BLENDMODE_NORMAL));
        Whiteboard cachedWhiteboard = whiteboardCache.getWhiteboard(boardId);
        assertEquals(4, cachedWhiteboard.getStrokes().size());
        assertEquals(BlendMode.BLENDMODE_ERASE, cachedWhiteboard.getStrokes().get(2).getBlendMode());
        assertEquals(BlendMode.BLENDMODE_NORMAL, cachedWhiteboard.getStrokes().get(3).getBlendMode());
        assertEquals(Color.GREEN, cachedWhiteboard.getStrokes().get(3).getColor());
    }

    @Test
    public void testIncomingRealtimeClearShouldClearCache() {
        String boardId = "cdbd6649-f6cd-416b-b529-656c7e8a1606";
        List<Stroke> strokeList = new ArrayList<>();
        strokeList.add(mock(Stroke.class));
        strokeList.add(mock(Stroke.class));
        strokeList.add(mock(Stroke.class));
        strokeList.add(mock(Stroke.class));
        WhiteboardCache whiteboardCache = new WhiteboardCache(bus, gson);
        whiteboardCache.initAndStartRealtimeForBoard(boardId, strokeList);

        WhiteboardRealtimeEvent event = new WhiteboardRealtimeEvent("{action: \"endClearBoard\"}");
        whiteboardCache.onEventBackgroundThread(event);
        assertEquals(0, whiteboardCache.getWhiteboard(boardId).getStrokes().size());
    }

    @Test
    public void testIncomingUnknownRealtimeMessages() {
        String boardId = "d28c56d7-c84f-4b0e-883f-5cda56646e09";
        List<Stroke> strokeList = new ArrayList<>();
        strokeList.add(new Stroke(new float[] { 11.0f, 200.0f, 1.0f, 50.0f, 30.0f, 1.0f }, Color.RED, STRIDE, BlendMode.BLENDMODE_NORMAL));
        WhiteboardCache whiteboardCache = new WhiteboardCache(bus, gson);
        whiteboardCache.initAndStartRealtimeForBoard(boardId, strokeList);

        whiteboardCache.onEventBackgroundThread(new WhiteboardRealtimeEvent("{action: \"contentBegin\"}"));
        whiteboardCache.onEventBackgroundThread(new WhiteboardRealtimeEvent("{action: \"contentUpdate\"}"));
        whiteboardCache.onEventBackgroundThread(new WhiteboardRealtimeEvent("{action: \"contentCancel\"}"));
        whiteboardCache.onEventBackgroundThread(new WhiteboardRealtimeEvent("{action: \"startClearBoard\"}"));
        whiteboardCache.onEventBackgroundThread(new WhiteboardRealtimeEvent("{action: \"loremIpsum\"}"));

        assertEquals(1, whiteboardCache.getWhiteboard(boardId).getStrokes().size());
    }

    @Test
    public void testOnlyLastInitiatedBoardShouldBeFresh() {
        String boardId1 = "f8581071-b9b5-49b7-8db9-a5facd184808";
        List<Stroke> strokes1 = new ArrayList<>();
        strokes1.add(mock(Stroke.class));
        strokes1.add(mock(Stroke.class));

        String boardId2 = "c6887aea-8689-4d27-beb7-f10dcb9a53bc";
        List<Stroke> strokes2 = new ArrayList<>();
        strokes2.add(mock(Stroke.class));

        String boardId3 = "7ce17f8a-cf89-4616-9b5c-2004c0c87b01";
        List<Stroke> strokes3 = new ArrayList<>();
        strokes3.add(mock(Stroke.class));
        strokes3.add(mock(Stroke.class));
        strokes3.add(mock(Stroke.class));
        strokes3.add(mock(Stroke.class));

        WhiteboardCache whiteboardCache = new WhiteboardCache(bus, gson);
        whiteboardCache.initAndStartRealtimeForBoard(boardId1, strokes1);
        assertFalse(whiteboardCache.getWhiteboard(boardId1).isStale());

        whiteboardCache.initAndStartRealtimeForBoard(boardId2, strokes2);
        assertTrue(whiteboardCache.getWhiteboard(boardId1).isStale());
        assertFalse(whiteboardCache.getWhiteboard(boardId2).isStale());

        whiteboardCache.initAndStartRealtimeForBoard(boardId3, strokes3);
        assertTrue(whiteboardCache.getWhiteboard(boardId1).isStale());
        assertTrue(whiteboardCache.getWhiteboard(boardId2).isStale());
        assertFalse(whiteboardCache.getWhiteboard(boardId3).isStale());
    }

    @Test
    public void testStart() {
        WhiteboardCache whiteboardCache = new WhiteboardCache(bus, gson);
        whiteboardCache.start();
        verify(bus).register(whiteboardCache);
    }

    @Test
    public void testStop() {
        WhiteboardCache whiteboardCache = new WhiteboardCache(bus, gson);
        whiteboardCache.stop();
        verify(bus).unregister(whiteboardCache);
    }

    private WhiteboardRealtimeEvent createContentCommitJson(float[] points, int color, BlendMode blendMode) {
        JsonObject realtimeMessage = new JsonObject();
        realtimeMessage.addProperty("action", "contentCommit");
        JsonArray curvePoints = new JsonArray();
        for (float point : points) {
            curvePoints.add(point);
        }
        realtimeMessage.add("curvePoints", curvePoints);
        realtimeMessage.add("color", WhiteboardUtils.convertColorIntToJson(color));
        realtimeMessage.addProperty("stride", STRIDE);
        realtimeMessage.addProperty("drawMode", (blendMode == BlendMode.BLENDMODE_ERASE ? "ERASE" : ""));

        return new WhiteboardRealtimeEvent(realtimeMessage.toString());
    }
}
