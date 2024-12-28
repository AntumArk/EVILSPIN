package evil.spin;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
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
import androidx.annotation.Nullable;
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
import java.util.stream.Collectors;

import kotlin.NotImplementedError;

public class MainActivity extends AppCompatActivity {
    private WheelView wheelView;
    private Button spinButton;
    private EditText optionInput;
    private Button addOptionButton;
    private Button settingsButton;
    private Button saveButton;
    private Button addWheelButton;
    private Button deleteWheelButton;
    private TextView titleBar;
    private DrawerLayout mainLayout;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private NavigationView navigationView;
    private Menu wheelMenu;
    private List<String> options = new ArrayList<>();
    private List<Wheel> Wheels = new ArrayList<Wheel>();
    private Wheel CurrentWheel;
    private final IWheelSerializer wheelSerializer = new WheelSerializer();
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
        setUpSettingsButtons();
        setUpSideMenu();

        try {
            loadWheels();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        prepareWheelMenu();

        if (Wheels.isEmpty())
            addWheel();
        else
            CurrentWheel = Wheels.iterator().next();

        loadOptions(CurrentWheel);

        addOptionButton.setOnClickListener(v -> addOption());
        spinButton.setOnClickListener(v -> spinWheel());
        settingsButton.setOnClickListener(v -> openSettings());
        RainbowBorderButtonDrawable rainbowDrawable = new RainbowBorderButtonDrawable(this);
        spinButton.setBackground(rainbowDrawable);
        updateWheelAppearance();
        updateTitle(CurrentWheel);
        updateBackground();
        checkAnimationsEnabled();
    }

    private void setUpSettingsButtons() {
        saveButton = findViewById(R.id.btn_save);
        deleteWheelButton = findViewById(R.id.btn_delete);
        addWheelButton = findViewById(R.id.btn_add_wheel);

        saveButton.setOnClickListener(v->saveWheels());
        deleteWheelButton.setOnClickListener(v->deleteWheel());
        addWheelButton.setOnClickListener(v->addWheel());
    }

    private void setUpSideMenu() {
        navigationView = findViewById(R.id.navigationView);

        wheelMenu = navigationView.getMenu();

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, mainLayout, R.string.nav_open, R.string.nav_close);
        mainLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        // Enable the home button to show the drawer toggle
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
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
       // FakeWheels(); // TODO remove after testing
        // Create a separate method for the menu item click listener
        MenuItem.OnMenuItemClickListener menuItemClicked = createMenuItemClickListener();

        for (Wheel wheel : Wheels) {
            wheelMenu.add(wheel.Name).setOnMenuItemClickListener(menuItemClicked);
        }

    }
    private MenuItem.OnMenuItemClickListener createMenuItemClickListener() {
        return new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
                // Handle menu item click here
                String wheelName = (String) menuItem.getTitle();
                Toast.makeText(MainActivity.this, wheelName + " clicked", Toast.LENGTH_SHORT).show();
                try {
                    CurrentWheel = findWheelByName(Wheels,(String) wheelName);
                    loadOptions(CurrentWheel);
                    updateTitle(CurrentWheel);
                    // Do something with the found wheel
                    Toast.makeText(MainActivity.this, "Found wheel: " + CurrentWheel.Name, Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    // Handle the exception
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    return true;
                }

                return true;
            }
        };
    }


    // Method to find a wheel by name
    public Wheel findWheelByName(List<Wheel> wheels, String name) throws Exception {
        // Filter wheels with the matching name
        List<Wheel> matchingWheels = wheels.parallelStream()
                .filter(wheel -> wheel.Name.equals(name))
                .collect(Collectors.toList());

        // Throw exception if no matching wheels or more than one matching wheel is found
        if (matchingWheels.isEmpty()) {
            throw new Exception("No wheel found with the name: " + name);
        } else if (matchingWheels.size() > 1) {
            throw new Exception("Multiple wheels found with the name: " + name);
        }

        // Return the single matching wheel
        return matchingWheels.get(0);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
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
    private void updateTitle(Wheel wheel) {
        titleBar.setText(wheel.Name);
        titleBar.refreshDrawableState();
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
            CurrentWheel.Options.add(option);
            wheelView.setOptions((List<String>) CurrentWheel.Options);
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
    private void loadOptions(Wheel wheel)
    {
        wheelView.setOptions((List<String>) wheel.Options);
    }
    private void updateWheel(Wheel wheel)
    {
        loadOptions(wheel);
        updateTitle(wheel);
    }
    private void saveWheels(){
        try {
            if(Wheels.parallelStream().anyMatch(wheel -> wheel.Name.equals(CurrentWheel.Name)))
            {
                new AlertDialog.Builder(this)
                        .setTitle("Same title found")
                        .setMessage("Overwrite?")
                        .setPositiveButton("OK", (dialog, which) -> {
                            try {
                                Wheel wheelToOverWrite = findWheelByName(Wheels,CurrentWheel.Name);
                                wheelToOverWrite.Options = CurrentWheel.Options;
                                CurrentWheel = wheelToOverWrite;
                                wheelSerializer.SaveWheelsToSharedPreferences(Wheels, sharedPreferences);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .show();
                return;
            }
            Wheels.add(CurrentWheel);
            wheelSerializer.SaveWheelsToSharedPreferences(Wheels, sharedPreferences);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void FakeWheels() {
        List<String> fakeoptions =  Arrays.asList("a","b","c");
        List<String> fakeoptions2 =  Arrays.asList("aa","ba","ca");
        Wheel wheel = new Wheel("Hello",fakeoptions);
        Wheels.add(wheel);
        Wheel wheel2 = new Wheel("ni",fakeoptions2);
        Wheels.add(wheel2);
        String json=wheel.Serialize();
    }

    private void loadWheels() throws JSONException {
        List<Wheel> loadedWheels=new ArrayList<>();
        loadedWheels = (List<Wheel>) wheelSerializer.LoadWheelsFromSharedPreferences(sharedPreferences);
    }

    private void addWheel() {
        CurrentWheel=new Wheel();
        updateWheel(CurrentWheel);
    }

    private void deleteWheel() {
        if(Wheels.contains(CurrentWheel)) {
            Wheels.remove(CurrentWheel);

            try {
                wheelSerializer.SaveWheelsToSharedPreferences(Wheels, sharedPreferences);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        addWheel();
    }
}