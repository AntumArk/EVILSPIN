package evil.spin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

import kotlin.NotImplementedError;

public class Wheel implements Serializable {
    public String Name = "";
    public Collection<String> Options = Collections.emptyList();
    public Wheel(String name, Collection<String> options)
    {
        Name = name;
        Options = options;
    }
    public Wheel() {}
    public String Serialize()
    {
        JSONObject json = new JSONObject();
        try {
            json.put("Name",Name);
            JSONArray options = new JSONArray(Options);
            json.put("Options",options);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return json.toString();
    }

}
