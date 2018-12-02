package com.ipower.tattoo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageButton;

public class BigTextButton extends ImageButton {
    String mText = "";
    String mFont = "";
    Paint mTextPaint;

    int mViewWidth;
    int mViewHeight;
    int mTextBaseline;
    int mAlign;
    int mColor;
    int mBaseline;

    public BigTextButton(Context context) {
        super(context);
        init();
    }

    public BigTextButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAttrs(attrs);
        init();
    }

    public BigTextButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseAttrs(attrs);
        init();
    }

    /**
     * Dig out Attributes to find text setting
     *
     * This could be expanded to pull out settings for textColor, etc if desired
     *
     * @param attrs
     */

    private void parseAttrs(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.BigTextButton);
        mText = a.getString(R.styleable.BigTextButton_text);
        mFont = a.getString(R.styleable.BigTextButton_font);
        mAlign = a.getInt(R.styleable.BigTextButton_alignment, 0);
        mColor = a.getColor(R.styleable.BigTextButton_textColor, 0xFFFFFFFF);
        mBaseline = a.getInt(R.styleable.BigTextButton_baseline, 0);
        a.recycle();

        /*for (int i = 0; i < attrs.getAttributeCount(); i++) {
            String s = attrs.getAttributeName(i);
            if (s.equalsIgnoreCase("text")) {
                mText = attrs.getAttributeValue(i);
            }
        }*/
    }

    public void setText(CharSequence text) {
        mText = text.toString();
        onSizeChanged(getWidth(), getHeight(), getWidth(), getHeight());
    }

    /**
     * initialize Paint for text, it will be modified when the view size is set
     */
    private void init() {
        mTextPaint = new TextPaint();
        mTextPaint.setColor(mColor);
        mTextPaint.setAntiAlias(true);

        if (mFont != null && !mFont.isEmpty()) {
            mTextPaint.setTypeface(Typeface.createFromAsset(getContext().getAssets(), mFont));
        }

        if (mAlign == 2) {
            mTextPaint.setTextAlign(Paint.Align.CENTER);
        } else if (mAlign == 1) {
            mTextPaint.setTextAlign(Paint.Align.RIGHT);
        } else {
            mTextPaint.setTextAlign(Paint.Align.LEFT);
        }
    }

    /**
     * set the scale of the text Paint objects so that the text will draw and
     * take up the full screen width
     */
    void adjustTextScale() {
        // do calculation with scale of 1.0 (no scale)
        mTextPaint.setTextScaleX(1.0f);
        Rect bounds = new Rect();
        // ask the paint for the bounding rect if it were to draw this
        // text.
        mTextPaint.getTextBounds(mText, 0, mText.length(), bounds);

        // determine the width
        int w = bounds.right - bounds.left;

        // calculate the baseline to use so that the
        // entire text is visible including the descenders
        int text_h = bounds.bottom - bounds.top;

        if (mBaseline == 1) {
            mTextBaseline = bounds.bottom + ((mViewHeight - text_h) / 2);
        } else {
            mTextBaseline = 0;
        }

        // determine how much to scale the width to fit the view
        float xscale = ((float) (mViewWidth - getPaddingLeft() - getPaddingRight())) / w;

        // set the scale for the text paint
        if (xscale < 1.0f) {
            mTextPaint.setTextScaleX(xscale - 0.05f);
            //mTextPaint.setTextSize(mTextPaint.getTextSize() * (xscale - 0.05f));
        }
    }

    /**
     * determine the proper text size to use to fill the full height
     */
    void adjustTextSize() {
        // using .isEmpty() isn't backward compatible with older API versions
        if (mText.length() == 0) {
            return;
        }

        mTextPaint.setTextSize(100);
        mTextPaint.setTextScaleX(1.0f);

        Rect bounds = new Rect();
        // ask the paint for the bounding rect if it were to draw this
        // text
        mTextPaint.getTextBounds(mText, 0, mText.length(), bounds);

        // get the height that would have been produced
        int h = bounds.bottom - bounds.top;

        // make the text text up 70% of the height
        float target;
        if (mBaseline == 1) {
            target = (float) mViewHeight * .7f;
        } else {
            target = (float) mViewHeight;
        }

        // figure out what textSize setting would create that height
        // of text
        float size = ((target / h) * 100f);

        // and set it into the paint
        mTextPaint.setTextSize(size);
    }

    /**
     * When the view size is changed, recalculate the paint settings to have the
     * text on the fill the view area
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // save view size
        mViewWidth = w;
        mViewHeight = h;

        // first determine font point size
        adjustTextSize();
        // then determine width scaling
        // this is done in two steps in case the
        // point size change affects the width boundary
        adjustTextScale();
        // we have changed this view, now we need to redraw
        // Note: redraw is not automatic if you are sending button clicks to this object
        // programmatically.
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // let the ImageButton paint background as normal
        super.onDraw(canvas);

        // draw the text
        // position is centered on width
        // and the baseline is calculated to be positioned from the
        // view bottom

        if (mAlign == 2) {
            canvas.drawText(mText, mViewWidth / 2, mViewHeight - mTextBaseline, mTextPaint);
        } else if (mAlign == 1) {
            canvas.drawText(mText, mViewWidth - getPaddingRight(), mViewHeight - mTextBaseline, mTextPaint);
        } else {
            canvas.drawText(mText, getPaddingLeft(), mViewHeight - mTextBaseline, mTextPaint);
        }
    }
}