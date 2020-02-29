package com.example.draw;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import static com.example.draw.PaintActivity.btSocket;
import static com.example.draw.PaintActivity.canvasBitmap;
import static java.lang.Math.abs;

public class DrawingView extends View{

    private static float BRUSH_SIZE = 2;
    private static float ERASE_SIZE = 5;
    private Path drawPath; // hold the path that will be drawn
    private Paint drawPaint, canvasPaint; // paint object to draw drawPath and drawCanvas
    private int paintColor = Color.BLACK; // initial color
    private Canvas drawCanvas; // canvas on which drawing takes place
  //  private Bitmap canvasBitmap; // canvas bitmap
    private boolean erase = false; // To enable and disable erasing mode
    private float mX, mY; // current touch
    private float mS_X, mS_Y; // start touch
    private byte[] mmBuffer; // mmBuffer store for the stream
    private List<String> list ;
    private boolean writing;

    public DrawingView(Context context, AttributeSet attrs){
        super(context, attrs);
        setUpDrawing();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // X and Y position of user touch.
        float touchX = event.getX();
        float touchY = event.getY();


        //add the touch to the list
        float x_bord = 10 +  (touchX/40) ;
        float y_bord = 5 + (touchY/40) ;
        String commend = x_bord + "," + y_bord + "@";
        list.add(commend);


        // Draw the path according to the touch event taking place.
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                touchUp();
                break;
            default:
                return false;
        }

        // invalidate the view so that canvas is redrawn.
        invalidate();
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
      //  canvasPaint.setTextSize(70);
       // drawCanvas.drawText("Some Text", 50, 120, canvasPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        canvas.drawPath(drawPath, drawPaint);

    }


    /**
     * Initialize all objects required for drawing here.
     * One time initialization reduces resource consumption.
     */
    private void setUpDrawing(){
        drawPath = new Path();
        drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        canvasPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG | Paint.SUBPIXEL_TEXT_FLAG );
        setBrushSize(BRUSH_SIZE);

        writing = false;
        mmBuffer = new byte[1];
        list = new LinkedList<String>();

        //thread for send commend to Arduino
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while(true) {
                        if(btSocket != null && !list.isEmpty()){
                            btSocket.getInputStream().read(mmBuffer);
                            if(mmBuffer[0]=='!'){
                                btSocket.getOutputStream().write((list.get(0)).getBytes());
                                list.remove(0);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }

    private void touchStart(float touchX, float touchY) {
        if(!writing){
            list.add("-");
            writing = true;
        }

        drawPath = new Path();
        drawPath.reset();
        drawPath.moveTo(touchX, touchY);
        drawPaint.setStyle(Paint.Style.STROKE);
        mX = touchX;
        mY = touchY;
        mS_X = touchX;
        mS_Y = touchY;

    }

    private void touchMove(float x, float y) {
        float dx = abs(x - mX);
        float dy = abs(y - mY);

        if (dx >= 1 || dy >= 1) {
            drawPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touchUp() {
        if(writing){
            list.add("_");
            writing = false;
        }

        if(erase){
            drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            if( (mX + 60 > mS_X &&  mS_X > mX - 60 ) && (mY + 60 > mS_Y &&  mS_Y > mY - 60 ) ){
                drawPath.lineTo(mS_X,mS_Y);
                drawPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            }
            else{
                drawPath.lineTo(mX, mY);
            }
        }
        else{
            drawPath.lineTo(mX, mY);
        }

        drawCanvas.drawPath(drawPath, drawPaint);
        drawPath.reset();
        drawPaint.setXfermode(null);
    }

    public void setErase(boolean isErase){
        erase = isErase;
        if(erase) {
            drawPaint.setColor(getResources().getColor(R.color.colorEraser));
            setBrushSize(ERASE_SIZE);
        }
        else {
            drawPaint.setColor(paintColor);
            setBrushSize(BRUSH_SIZE) ;
            drawPaint.setXfermode(null);
        }
    }

    public void setBrushSize(float newSize){
        float pixelAmount = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                newSize, getResources().getDisplayMetrics());
        drawPaint.setStrokeWidth(pixelAmount);
    }

    public void startNew(){
        drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        invalidate();
    }
}
