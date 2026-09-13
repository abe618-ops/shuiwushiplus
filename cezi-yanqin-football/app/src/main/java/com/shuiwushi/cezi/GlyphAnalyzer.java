package com.shuiwushi.cezi;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

public final class GlyphAnalyzer {
    private GlyphAnalyzer() {}

    public static final class Metrics {
        public long total;
        public long left, right, top, bottom;
        public long tl, tr, bl, br, center;
        public int minX, maxX, minY, maxY;
        public double density;

        public double leftRightBalance() {
            return total == 0 ? 0 : (left - right) / (double) total;
        }

        public double topBottomBalance() {
            return total == 0 ? 0 : (top - bottom) / (double) total;
        }
    }

    public static Metrics analyze(String ch) {
        final int size = 192;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTextSize(160f);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.LEFT);

        Rect bounds = new Rect();
        paint.getTextBounds(ch, 0, ch.length(), bounds);
        float x = (size - bounds.width()) / 2f - bounds.left;
        float y = (size - bounds.height()) / 2f - bounds.top;
        canvas.drawText(ch, x, y, paint);

        Metrics m = new Metrics();
        m.minX = size; m.minY = size; m.maxX = -1; m.maxY = -1;
        int[] pixels = new int[size * size];
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size);
        int mid = size / 2;
        int c0 = size * 3 / 8, c1 = size * 5 / 8;
        for (int yy = 0; yy < size; yy++) {
            for (int xx = 0; xx < size; xx++) {
                int a = Color.alpha(pixels[yy * size + xx]);
                if (a < 40) continue;
                m.total += a;
                if (xx < mid) m.left += a; else m.right += a;
                if (yy < mid) m.top += a; else m.bottom += a;
                if (xx < mid && yy < mid) m.tl += a;
                else if (xx >= mid && yy < mid) m.tr += a;
                else if (xx < mid) m.bl += a;
                else m.br += a;
                if (xx >= c0 && xx < c1 && yy >= c0 && yy < c1) m.center += a;
                if (xx < m.minX) m.minX = xx;
                if (xx > m.maxX) m.maxX = xx;
                if (yy < m.minY) m.minY = yy;
                if (yy > m.maxY) m.maxY = yy;
            }
        }
        if (m.maxX >= m.minX && m.maxY >= m.minY) {
            long boxArea = (long)(m.maxX - m.minX + 1) * (m.maxY - m.minY + 1) * 255L;
            m.density = boxArea == 0 ? 0 : m.total / (double) boxArea;
        }
        bitmap.recycle();
        return m;
    }
}
