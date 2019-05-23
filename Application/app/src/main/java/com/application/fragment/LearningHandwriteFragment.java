package com.application.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.application.R;

/*
 * 지수 작성
 * < 손글씨 화면 거기>
 * 1. 캔버스 나오게
 * 2. 그림자 글씨 나오게
 * 3. 지우개 버튼 먹게*
 *
 * 20190521 - 화면은 뜨는데 뷰가 생기지는 X
 * 20190522 - 캔버스 생성 성공
 */

public class LearningHandwriteFragment extends Fragment {
    DrawCanvasView shadowCanvasV;

    public LearningHandwriteFragment() {
    }

    public static LearningSummaryFragment newInstance() {
        return new LearningSummaryFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.fragment_learning_handwrite, container, false);
        init(view);
        //shadowCanvasV = (DrawCanvasView) view.findViewById(R.id.shadowCanvas);

        //Button 정의
        Button pen = (Button)view.findViewById(R.id.penButton);
        Button eraser = (Button) view.findViewById(R.id.eraserButton);

        eraser.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                shadowCanvasV.eraser();
            }
        });
        pen.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                shadowCanvasV.pen();
                init(view);
            }
        });

        return view;
    }

    public void init(View view) {
        shadowCanvasV = (DrawCanvasView) view.findViewById(R.id.shadowCanvas);
    }
}