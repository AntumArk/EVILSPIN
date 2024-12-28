package evil.spin;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.ActionMenuItem;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private WheelView wheelView;
    private Button spinButton;
    private EditText optionInput;
    private Button addOptionButton;
    private Button settingsButton;
    private TextView titleBar;
    private DrawerLayout mainLayout;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private NavigationView navigationView;
    private Menu wheelMenu;
    public androidx.appcompat.widget.Toolbar toolbar;
    private List<String> options = new ArrayList<>();
    private Collection<Wheel> Wheels = new ArrayList<Wheel>();
    private IWheelSerializer wheelSerializer = new WheelSerializer();
    private SharedPreferences sharedPreferences;
    private boolean wheelIsSpinning = false;

    private static final int SETTINGS_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        wheelView = findViewById(R.id.wheelView);
        spinButton = findViewById(R.id.spinButton);
        optionInput = findViewById(R.id.optionInput);
        addOptionButton = findViewById(R.id.addOptionButton);
        settingsButton = findViewById(R.id.settingsButton);
        titleBar = findViewById(R.id.titlebar);
        mainLayout = findViewById(R.id.activity_main);
        navigationView = findViewById(R.id.navigationView);
        wheelMenu = navigationView.getMenu();

        toolbar = findViewById(R.id.toolbar);
        this.setSupportActionBar(toolbar);

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, mainLayout, R.string.nav_open, R.string.nav_close);
        mainLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        // Enable the home button to show the drawer toggle
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        addOptionButton.setOnClickListener(v -> addOption());
        spinButton.setOnClickListener(v -> spinWheel());
        settingsButton.setOnClickListener(v -> openSettings());
        RainbowBorderButtonDrawable rainbowDrawable = new RainbowBorderButtonDrawable(this);
        spinButton.setBackground(rainbowDrawable);
        loadOptions();
        updateWheelAppearance();
        updateTitle();
        updateBackground();
        checkAnimationsEnabled();

        //saveWheels();
        try {
            loadWheels();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        prepareWheelMenu();
        navigationView.setNavigationItemSelectedListener(v->stuff());
    }

    private boolean stuff() {
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        this.wheelMenu = menu;
        getMenuInflater().inflate(R.menu.wheel_menu, menu);
        return true;
    }
    private void prepareWheelMenu() {
        // Clean menu from previous wheels
        wheelMenu.clear();
        FakeWheels(); // TODO remove after testing
        MenuItem.OnMenuItemClickListener menuItemClicked = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
                // Handle menu item click here
                Toast.makeText(MainActivity.this, menuItem.getTitle() + " clicked", Toast.LENGTH_SHORT).show();
                return true;
            }
        };
        for (Wheel wheel : Wheels) {
            wheelMenu.add(wheel.Name).setOnMenuItemClickListener(menuItemClicked);
        }
        //mainLayout.setAct
        // drawer layout instance to toggle the menu icon to open
        // drawer and back button to close drawer
//        drawerLayout = findViewById(R.id.activity_main);
//        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);
//
//        // pass the Open and Close toggle for the drawer layout listener
//        // to toggle the button
//        drawerLayout.addDrawerListener(actionBarDrawerToggle);
//        actionBarDrawerToggle.syncState();

        // to make the Navigation drawer icon always appear on the action bar
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    // override the onOptionsItemSelected()
    // function to implement
    // the item click listener callback
    // to open and close the navigation
    // drawer when the icon is clicked

    private void checkAnimationsEnabled() {
        boolean animationsEnabled = Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1) != 0;
        if (!animationsEnabled) {
            new AlertDialog.Builder(this)
                    .setTitle("Animation Disabled")
                    .setMessage("Animations are currently turned off. Please enable them in the device settings for the wheel to animate.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        updateTitle();
        updateBackground();
    }

    private void updateTitle() {
        String title = sharedPreferences.getString("wheel_title", "Spin the Wheel");
        titleBar.setText(title);
    }

    private void updateBackground() {
        String background = sharedPreferences.getString("background", "red");
        setBackgroundByName(background);
    }
    private void setBackgroundByName(String backgroundName) {
        try {
            String resourceName = "screen_" + backgroundName.toLowerCase();
            int resourceId = getResources().getIdentifier(resourceName, "drawable", getPackageName());
            if (resourceId != 0) {
                mainLayout.setBackgroundResource(resourceId);
            } else {
                // Fallback to default background if the resource is not found
                mainLayout.setBackgroundResource(R.drawable.screen_red);
            }
        } catch (Resources.NotFoundException e) {
            // Fallback to default background if there's an error
            mainLayout.setBackgroundResource(R.drawable.screen_red);
        }
    }

    private void addOption() {
        String option = optionInput.getText().toString().trim();
        if (!option.isEmpty()) {
            options.add(option);
            wheelView.setOptions(options);
            optionInput.setText("");
            saveOptions();
        }
    }
    private void clearEditTextFocus() {
        optionInput.clearFocus();
        // This will hide the soft keyboard if it's visible
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(optionInput.getWindowToken(), 0);
    }
    private void spinWheel() {
        clearEditTextFocus();
        if (options.isEmpty()) {
            Toast.makeText(this, "Please add options before spinning", Toast.LENGTH_SHORT).show();
            return;
        }
        if(wheelIsSpinning) return;
        
        int minSpin = sharedPreferences.getInt("min_wheel_spin", 720);
        int maxSpin = minSpin + 1080;
        Random random = new Random();
        float targetRotation = wheelView.getRotation() + random.nextInt(maxSpin - minSpin) + minSpin;

        int duration = sharedPreferences.getInt("wheel_speed", 5000); // Increased duration for a slower finish
        ValueAnimator animator = ValueAnimator.ofFloat(wheelView.getRotation(), targetRotation);
        animator.setDuration(duration);

        // Use a custom interpolator for fast start and gradual slowdown
        animator.setInterpolator(input -> {
            return (float) (1 - Math.pow(1 - input, 3)); // Ease-out cubic
        });

        animator.addUpdateListener(animation -> {
            float value = (Float) animation.getAnimatedValue();
            wheelView.setRotation(value);
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                showResult(wheelView.getSelectedOption());
                wheelIsSpinning = false;
            }
        });

        animator.start();
        wheelIsSpinning = true;
    }

    private void showResult(String winner) {
        new AlertDialog.Builder(this)
                .setTitle("Result")
                .setMessage("The wheel landed on: " + winner)
                .setPositiveButton("OK", null)
                .show();
    }

    private void openSettings() {
        clearEditTextFocus();
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, SETTINGS_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            updateWheelAppearance();
            loadOptions();
        }
    }

    private void updateWheelAppearance() {
        String colorPalette = sharedPreferences.getString("color_palette", "Default");
        wheelView.setColorPalette(colorPalette);
    }

    private void saveOptions() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("wheel_options", new HashSet<>(options));
        editor.apply();
    }

    private void loadOptions() {
        Set<String> savedOptions = sharedPreferences.getStringSet("wheel_options", new HashSet<>());
        options = new ArrayList<>(savedOptions);
        wheelView.setOptions(options);
    }
    private void saveWheels(){

        try {
            wheelSerializer.SaveWheelsToSharedPreferences(Wheels, sharedPreferences);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void FakeWheels() {
        List<String> fakeoptions =  Arrays.asList("a","b");
        Wheel wheel = new Wheel("Hello",fakeoptions);
        Wheels.add(wheel);
        Wheel wheel2 = new Wheel("ni",fakeoptions);
        Wheels.add(wheel);
        Wheels.add(wheel2);
        String json=wheel.Serialize();
    }

    private void loadWheels() throws JSONException {
        List<Wheel> loadedWheels=new ArrayList<>();
        loadedWheels = (List<Wheel>) wheelSerializer.LoadWheelsFromSharedPreferences(sharedPreferences);
    }
}