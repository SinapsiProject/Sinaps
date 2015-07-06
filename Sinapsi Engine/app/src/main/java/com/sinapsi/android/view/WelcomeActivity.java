package com.sinapsi.android.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.sinapsi.android.SinapsiAndroidApplication;
import com.sinapsi.client.AppConsts;
import com.sinapsi.android.R;


/**
 * This activity is the main activty, it gives the user the option to view
 * a tutorial of sinapsi, or the option to skip and login/register into the system
 *
 */
public class WelcomeActivity extends Activity {

    private static final int TOTAL_ANIM_DURATION = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set the layout
        if(AppConsts.DEBUG_BYPASS_LOGIN){
            Intent i = new Intent(WelcomeActivity.this, MainActivity.class);
            ((SinapsiAndroidApplication) getApplication()).setLoggedIn(true);
            startActivity(i);
        }
        setContentView(R.layout.welcome_layout);



        TextView signUp = (TextView) findViewById(R.id.signUp_text);
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent register = new Intent(WelcomeActivity.this, RegisterActivity.class);
                startActivityForResult(register, 0);
            }
        });


        TextView signIn = (TextView) findViewById(R.id.signIn_text);
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent login = new Intent(WelcomeActivity.this, LoginActivity.class);
                startActivityForResult(login, 0);
            }
        });

        ImageView logo = (ImageView) findViewById(R.id.logo);

        Animation alpha1 = AnimationUtils.loadAnimation(this, R.anim.anim_alpha);
        alpha1.setDuration(TOTAL_ANIM_DURATION/3);
        alpha1.setFillAfter(true);

        Animation alpha2 = AnimationUtils.loadAnimation(this, R.anim.anim_alpha);
        alpha1.setFillAfter(true);
        alpha2.setStartOffset(alpha1.getDuration());
        alpha2.setDuration(TOTAL_ANIM_DURATION/3);

        Animation alpha3 = AnimationUtils.loadAnimation(this, R.anim.anim_alpha);
        alpha1.setFillAfter(true);
        alpha3.setStartOffset(alpha1.getDuration()+alpha2.getDuration());
        alpha3.setDuration(TOTAL_ANIM_DURATION/3);


        logo.startAnimation(alpha1);
        signUp.startAnimation(alpha2);
        signIn.startAnimation(alpha3);


        startService(new Intent(getApplicationContext(), com.sinapsi.android.background.SinapsiBackgroundService.class));

    }
}
