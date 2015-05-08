package edu.spu.teamroot.voicecloud;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.util.Log;
import android.widget.RelativeLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

public class WordCloud {
    /*
     * Static members
     */

    private static WordCloud instance;
    public static Context context;
    public static RelativeLayout layout; // The layout containing all word views

    /*
     * Static methods
     */

    public static WordCloud createInstance(Context context, RelativeLayout layout) {
        if (instance == null) {
            instance = new WordCloud(context, layout);
        }

        return instance;
    }

    public static WordCloud getInstance() {
        return instance;
    }
    public static void deleteInstance() {
        instance = null;
    }

    /*
     * Member variables
     */

    private Map<String, Word> wordList;

    private Deque<WordGroup> freeGroups;
    private int groupSize;

    private WordGroup wordTreeRoot;

    private long timestamp;

    /*
     * Constructors
     */

    private WordCloud(Context context, RelativeLayout layout) {
        WordCloud.context = context;
        WordCloud.layout = layout;

        wordList = new TreeMap<>();
        freeGroups = new LinkedList<>();
        groupSize = 0;
        wordTreeRoot = new WordGroup();
        timestamp = System.currentTimeMillis();

        int width, height;

        if (layout.getWidth() == 0 && layout.getHeight() == 0) {
            // The layout does not have a size... it likely has not been drawn yet.
            // We can use the layout params to get the initial size at least
            width = UnitConverter.getInstance().toDp(layout.getLayoutParams().width);
            height = UnitConverter.getInstance().toDp(layout.getLayoutParams().height);
        } else {
            width = UnitConverter.getInstance().toDp(layout.getWidth());
            height = UnitConverter.getInstance().toDp(layout.getHeight());
        }

        wordTreeRoot.setBounds(new Rect(width / 2, height / 2, width / 2, height / 2));
        Log.d("wordTreeRoot", "Width: " + width + " Height: " + height);
    }

    /*
     * Methods
     */

    public void addWord(String word) {
        addWord(word, 1);
    }

    public void addWord(String name, int count) {
        Word word = wordList.get(name);

        if (word == null) {
            // This will create a new button (if eligible) and set the initial size of the word
            word = new Word(name, count);

            // Add to master word list
            wordList.put(word.getName(), word);

            if (word.isButtonCreated()) {
                attachWord(word);
                repositionWord(word, true);
            }
        } else {
            // Increment only the count (this will create word if needed)
            if (word.incrementCount(count)) {
                if (word.isButtonCreated()) {
                    if (!word.isWordAttached()) {
                        attachWord(word);
                        repositionWord(word, true);
                    } else {
                        // Word exists in the tree already, position relative to self
                        repositionWord(word, false);
                    }
                }
            } else {
                // Count is <= 0, remove the word from the tree!
                WordCloud.getInstance().removeWord(word);
            }
        }
    }

    public Word getWord(String name) {
        return wordList.get(name);
    }

    public long getTimestamp() { return this.timestamp; }

    // Adds a word to the word cloud. Finds a free group, and increases group size if needed.
    private void attachWord(Word word) {
        // Calculate new group size ( 1 + floor( sqrt( n-1 ) ) )
        // e.g. n=4, size = 2 groups of 2; n=8, size = 3 groups of 3
        int newSize = 1 + (int)Math.floor(Math.sqrt(wordList.size() - 1));

        // If size needs to be increased, or there are no free groups (for some reason)
        if (newSize > groupSize || freeGroups.isEmpty()) {
            // Update to newSize, or increment count if we need a new group
            if (newSize > groupSize) {
                groupSize = newSize;
            } else {
                groupSize++;
            }

            // Update free groups list (group size has changed)
            for (WordGroup group : wordTreeRoot.children) {
                if (group.children.size() < groupSize) {
                    freeGroups.addFirst(group);
                }
            }

            // Create new word group and link to tree hierarchy
            WordGroup newGroup = new WordGroup();
            newGroup.addChild(word);
            wordTreeRoot.addChild(newGroup);

            // Add new group to front of free group queue (most likely has extra space)
            if (newGroup.children.size() < groupSize) {
                freeGroups.addFirst(newGroup);
            }
        } else {
            // Get a free group and add the word to it
            WordGroup group = freeGroups.removeFirst();
            group.addChild(word);

            // Stick group back if still has room
            if (group.children.size() < groupSize) {
                freeGroups.addFirst(group);
            }
        }
    }

    // Repositions a word inside a group.
    private void repositionWord(Word word, boolean initialPlacement) {
        if (!word.isWordAttached()) return;

        // Initially, reposition word in parent group (otherwise relative to current position)
        word.parent.repositionChild(word, initialPlacement);

        // Now, reposition parent group within the root.
        // If size <= 1, give it an initial position (once we get more children, we cannot move around as much)
        wordTreeRoot.repositionChild(word.parent, word.parent.children.size() <= 1);

        // Make sure the word is shown (can be hidden on create)
        word.show();
    }

    // Removes a word, deleting it from the word cloud.
    public void removeWord(Word word) {
        removeWord(word, true);
    }

    private void removeWord(Word word, boolean deleteFromList) {
        WordGroup group = word.parent;

        if (deleteFromList) {
            wordList.remove(word.getName()); // Remove from word list
        }

        word.delete(); // This will remove from tree and delete button

        if (group != null) {
            // Refresh group bounds
            group.bounds.setEmpty();

            for (WordGroup child : group.children) {
                group.bounds.union(child.bounds);
            }

            // Add parent to list of free groups
            if (group.children.size() < groupSize && !freeGroups.contains(group)) {
                freeGroups.addFirst(group);
            }
        }
    }

    // Clears the word cloud.
    public void clear() {
        Iterator it = wordList.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();

            // Do not remove it from the list here... this will cause an exception!
            removeWord((Word)pair.getValue(), false);

            it.remove(); // Proper way to delete when using iterators
        }

        // Update timestamp
        timestamp = System.currentTimeMillis();
    }

    public boolean isWordInCloud(String name) {
        return wordList.containsKey(name);
    }

    public void uploadWordCloud() {

        Log.d("wordCloud", "Saving word cloud");

        // Create Master JSON Object
        JSONObject toSend = new JSONObject();

        // Create layout JSON Object
        JSONObject layout = new JSONObject();
        int width = UnitConverter.getInstance().toDp(this.layout.getLayoutParams().width);
        int height = UnitConverter.getInstance().toDp(this.layout.getLayoutParams().height);
        String timestamp = new Timestamp(this.getTimestamp()).toString();

        try {
            layout.put("width", width);
            layout.put("height", height);
            layout.put("timestamp", timestamp);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Create Words JSON Array
        JSONArray words = new JSONArray();

        // Iterate through WordList and add to Json object
        for (Object o : wordList.entrySet()) {
            Map.Entry pair = (Map.Entry) o;

            // Get current word
            Word currWord = (Word) pair.getValue();

            // Create JSON Object for word
            JSONObject word = new JSONObject();

            // Put current word cloud data into json object
            try {
                // Add all word properties
                word.put("name", currWord.getName());
                word.put("count", currWord.getCount());
                word.put("timestamp", new Timestamp(currWord.getTimestamp()).toString());

                // Convert rect to JSON
                JSONObject bounds = new JSONObject();
                bounds.put("bottom", UnitConverter.getInstance().toDp(currWord.getBounds().bottom));
                bounds.put("left", UnitConverter.getInstance().toDp(currWord.getBounds().left));
                bounds.put("right", UnitConverter.getInstance().toDp(currWord.getBounds().right));
                bounds.put("top", UnitConverter.getInstance().toDp(currWord.getBounds().top));
                word.put("bounds", bounds);

                // Get color as string
                //Drawable buttonBackground = currWord.button.getBackground();
                //word.put("color", buttonBackground);

                // Add word json to words array
                words.put(word);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        try {

            // Add layout and words array to master json object
            toSend.put("layout", layout);
            toSend.put("words", words);

            // Transmit object to web server
            JSONTransmitter transmitter = new JSONTransmitter();
            transmitter.execute(toSend);

            JSONObject result = transmitter.get();
            String cloudID = result.getString("id");

            // Toast result
            //Toast.makeText(WordCloud.context, "ID: "+ cloudID, Toast.LENGTH_SHORT).show();

            AlertDialog.Builder builder = new AlertDialog.Builder(WordCloud.context);
            builder.setMessage("View your word cloud by visiting \"http://52.24.35.51/cloud\" and entering the CloudID: " + cloudID)
                    .setTitle("Word Cloud Saved")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //do things
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
