package com.cherrydev.chirpchain.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.cherrydev.chirpchain.R;

/**
 * Created by alannon on 2015-02-28.
 */
public class ProgressView extends View {
    Drawable bracketOrange;
    Drawable bracketYellow;
    Paint p = new Paint();
    int bracketCount = 40;
    float progress;
    public ProgressView(Context context) {
        super(context);
        bracketOrange = context.getResources().getDrawable(R.drawable.bracketorange);
        bracketYellow = context.getResources().getDrawable(R.drawable.bracketyellow);
        p.setColor(0xffffffff);
    }

    public void setProgress(float progress) {
        this.progress = progress;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int eachWidth = width / bracketCount;
        for (int i = 0; i < bracketCount; i++) {
            if ( ((float)i) / bracketCount <= progress ) {
                bracketOrange.setBounds(i * eachWidth, 0, i * eachWidth + eachWidth, 50);
                bracketOrange.draw(canvas);
            }
            else {
                bracketYellow.setBounds(i * eachWidth, 0, i * eachWidth + eachWidth, 50);
                bracketYellow.draw(canvas);
            }
        }
    }
}
