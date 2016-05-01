package net.xaxxi.locationtracker;

import android.support.v4.app.Fragment;
import android.os.Bundle;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;

import android.util.Log;

public class HelloFragment extends Fragment {

    Animation mAnimCalendarIn;
    Animation mAnimCalendarOut;

    public HelloFragment() {
        Log.d("==================================================",
              "HelloFragment constructed");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceSate) {
        View v = inflater.inflate(R.layout.fragment_hello, container, false);

        /*
        mAnimCalendarIn =
            (Animation)AnimationUtils.loadAnimation(v.getContext(),
                                                    R.anim.calendar_in);
        mAnimCalendarOut =
            (Animation)AnimationUtils.loadAnimation(v.getContext(),
                                                    R.anim.calendar_out);
        */
        return v;
    }
}
