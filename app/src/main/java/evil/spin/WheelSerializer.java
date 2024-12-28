package evil.spin;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class WheelSerializer implements IWheelSerializer {
    private final String SharedPreferencesWheelsKey = "Wheels";
    @Override
    public String SerializeWheels(Collection<Wheel> wheels) throws JSONException {
        List<JSONObject> wheelsJsonObj = new ArrayList<>();
        for(Wheel wheel:wheels)
        {
            JSONObject wheelObj =new JSONObject(wheel.Serialize());
            wheelsJsonObj.add(wheelObj);
        }
        JSONArray wheelsJsonArray = new JSONArray(wheelsJsonObj);
        JSONObject wheelsJson = new JSONObject();
        wheelsJson.put(SharedPreferencesWheelsKey,wheelsJsonArray);
        return wheelsJson.toString();
    }

    @Override
    public void SaveWheelsToSharedPreferences(Collection<Wheel> wheels, SharedPreferences preferences) throws JSONException {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SharedPreferencesWheelsKey, SerializeWheels(wheels));
        editor.apply();
    }

    @Override
    public Collection<Wheel> LoadWheelsFromSharedPreferences(SharedPreferences preferences) throws JSONException {
        String wheelsJson = preferences.getString(SharedPreferencesWheelsKey,"");

        return DeserializeWheels(wheelsJson);
    }

    @Override
    public Collection<Wheel> DeserializeWheels(String json) throws JSONException {
        if(json.isEmpty()) return new ArrayList<>();
        JSONObject wheelsJson = new JSONObject(json);

        JSONArray wheelsJsonArray = wheelsJson.getJSONArray(SharedPreferencesWheelsKey);
        return getWheelsFromJsonArray(wheelsJsonArray);
    }

    @NonNull
    @Override
    public List<Wheel> getWheelsFromJsonArray(JSONArray wheelsJsonArray) throws JSONException {
        List<Wheel> wheels = new ArrayList<>();
        for(int i = 0; i< wheelsJsonArray.length(); i++)
        {
            JSONObject wheelJson = wheelsJsonArray.getJSONObject(i);
            Wheel wheel = getWheelFromJson(wheelJson);
            wheels.add(wheel);
        }
        return wheels;
    }

    @NonNull
    @Override
    public Wheel getWheelFromJson(JSONObject wheelJson) throws JSONException {
        String[] options = toStringArray(wheelJson.getJSONArray("Options"));
        String wheelName = wheelJson.getString("Name");
        return new Wheel(wheelName, Arrays.asList(options));
    }

    @Override
    public String[] toStringArray(JSONArray array) {
        if(array==null)
            return new String[0];

        String[] arr=new String[array.length()];
        for(int i=0; i<arr.length; i++) {
            arr[i]=array.optString(i);
        }
        return arr;
    }
}
