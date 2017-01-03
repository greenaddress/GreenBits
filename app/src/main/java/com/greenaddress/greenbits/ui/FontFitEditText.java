package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.EditText;

public class FontFitEditText extends EditText {

    //Attributes
    private Paint mTestPaint;
    private float mTextSizeMax;
    private float mTextSizeMin;

    public FontFitEditText(Context context) {
        super(context);
        initialise();
    }

    public FontFitEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FontFitEditText);
        mTextSizeMax = typedArray.getDimensionPixelSize(R.styleable.FontFitEditText_textSizeMax, 0);
        mTextSizeMax = pxToDp(mTextSizeMax);
        mTextSizeMin = typedArray.getDimensionPixelSize(R.styleable.FontFitEditText_textSizeMin, 0);
		if (typedArray != null) {
			typedArray.recycle();
		}
        mTextSizeMin = pxToDp(mTextSizeMin);
        initialise();
    }

    private void initialise() {
        mTestPaint = new Paint();
        mTestPaint.set(this.getPaint());
        //max size defaults to the initially specified text size unless it is too small
    }

    private float getScale() {
        return (getResources().getDisplayMetrics().densityDpi / 160);
    }

    private float pxToDp(float px) {
        return px / getScale();
    }

    private float dpiToPx(float px) {
        return px * getScale();
    }

    /* Re size the font so the specified text fits in the text box
     * assuming the text box is the specified width.
     */
    private void refitText(String text, int textWidth) {
        if (textWidth <= 0)
            return;
        int targetWidth = textWidth - this.getPaddingLeft() - this.getPaddingRight();
        targetWidth = (int)pxToDp(targetWidth);

        // font size in dp
        float hi = (mTextSizeMax > 0 && mTextSizeMax > mTextSizeMin) ? mTextSizeMax : 28;
        float lo = (mTextSizeMin > 0 && mTextSizeMin < hi) ? mTextSizeMin : 16;

        final float threshold = 0.5f; // How close we have to be

        mTestPaint.set(this.getPaint());

        while ((hi - lo) > threshold) {
            float sizeDpi = (hi + lo) / 2;
            float sizePx = dpiToPx(sizeDpi);
            mTestPaint.setTextSize(sizePx);
            if (pxToDp(mTestPaint.measureText(text)) >= targetWidth)
                hi = sizeDpi; // too big
            else
                lo = sizeDpi; // too small
        }
        // Use lo so that we undershoot rather than overshoot
        this.setTextSize(TypedValue.COMPLEX_UNIT_DIP, lo);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int height = getMeasuredHeight();
        refitText(UI.getText(this), parentWidth);
        this.setMeasuredDimension(parentWidth, height);
    }

    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int before, final int after) {
        refitText(text.toString(), this.getWidth());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w != oldw)
            refitText(UI.getText(this), w);
    }
}
