package work.technie.popularmovies;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.GridView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by anupam on 4/12/15.
 */
public class MainActivityFragment extends Fragment {
    private MovieArrayAdapter movieListAdapter;

    public MainActivityFragment(){

    }
    private void updateMovieList() {
        FetchMovieTask weatherTask = new FetchMovieTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sortingOrder = prefs.getString(getString(R.string.pref_sort_key),
                getString(R.string.pref_sort_default));
        weatherTask.execute(sortingOrder);
    }
    @Override
    public void onStart() {
        super.onStart();
        updateMovieList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState){

        // The ArrayAdapter will take data from a source and
        // use it to populate the ListView it's attached to.
        movieListAdapter =
                new MovieArrayAdapter(
                        getActivity(), // The current context (this activity)
                        new ArrayList<MovieInfo>());

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        final GridView gridView = (GridView) rootView.findViewById(R.id.gridview_movie);
        gridView.setAdapter(movieListAdapter);
        Log.d("MainActivityFrag", "Value: " + gridView.getWidth());

        gridView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                final int size= gridView.getWidth();
                int numCol=(int)Math.round((double)size /(double) getResources().getDimensionPixelSize(R.dimen.poster_width));
                gridView.setNumColumns(numCol);
                //Log.d("MainActivityFrag", "Value: " +size+" "+numCol+" "+getResources().getDimensionPixelSize(R.dimen.poster_width)+" "+test);
            }
        });

        //gridView.setNumColumns(5);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                MovieInfo movieInfo = (MovieInfo) adapterView.getItemAtPosition(position);

                String MOVIEID = movieInfo.id;
                String ORGLANG=movieInfo.orgLang;
                String ORGTITLE=movieInfo.orgTitle;
                String OVER=movieInfo.overview;
                String RELDATE=movieInfo.relDate;
                String POSTURL=movieInfo.postURL;
                String POPULARITY=movieInfo.popularity;
                String VOTAVG=movieInfo.votAvg;

                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra("movieId", MOVIEID)
                        .putExtra("orgLang", ORGLANG)
                        .putExtra("orgTitle", ORGTITLE)
                        .putExtra("overview", OVER)
                        .putExtra("relDate", RELDATE)
                        .putExtra("postUrl", POSTURL)
                        .putExtra("popularity", POPULARITY)
                        .putExtra("voteAvg", VOTAVG)
                        ;
                startActivity(intent);
            }
        });

        return rootView;
    }

    public class FetchMovieTask extends AsyncTask<String, Void, ArrayList<MovieInfo>> {

        private final String LOG_TAG = FetchMovieTask.class.getSimpleName();

        /**
         * Take the String representing the complete movie list in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private ArrayList<MovieInfo> getMovieDataFromJson(String movieJsonStr)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String ID="id";
            final String ORGLANG="original_language";
            final String ORGTITLE="original_title";
            final String OVER="overview";
            final String RELDATE="release_date";
            final String POSTERPATH="poster_path";
            final String POPULARITY="popularity";
            final String VOTAVG="vote_average";

            final String RESULT="results";
            final String POSTER_BASE_URL="http://image.tmdb.org/t/p/w185";

            JSONObject movieJson = new JSONObject(movieJsonStr);
            JSONArray movieArray = movieJson.getJSONArray(RESULT);

            ArrayList<MovieInfo> resultStrs=new ArrayList<>();

            for(int i = 0; i < movieArray.length(); i++) {

                String id;
                String orgLang;
                String orgTitle;
                String overview;
                String relDate;
                String postURL;
                String popularity;
                String votAvg;

                JSONObject movieInfo = movieArray.getJSONObject(i);

                id=movieInfo.getString(ID);
                orgLang=movieInfo.getString(ORGLANG);
                orgTitle=movieInfo.getString(ORGTITLE);
                overview=movieInfo.getString(OVER);
                relDate=movieInfo.getString(RELDATE);

                postURL= Uri.parse(POSTER_BASE_URL).buildUpon().
                        appendEncodedPath(movieInfo.getString(POSTERPATH)).build().toString();
                //Log.d("MainActivity", "Value: " + postURL);
                popularity=movieInfo.getString(POPULARITY);
                votAvg=movieInfo.getString(VOTAVG);

                resultStrs.add(new MovieInfo(id,orgLang,orgTitle,overview,relDate,
                        popularity,votAvg,postURL));
            }
            return resultStrs;

        }
        @Override
        protected ArrayList<MovieInfo> doInBackground(String... params) {

            if (params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String movieJsonStr = null;

            try {
                // Construct the URL for the movieAPI query
                // Possible parameters are avaiable at OWM's forecast API page, at

                final String MOVIE_BASE_URL =
                        "http://api.themoviedb.org/3/discover/movie?";
                final String SORT_PARAM = "sort_by";
                final String APPID_PARAM = "api_key";

                Uri builtUri = Uri.parse(MOVIE_BASE_URL).buildUpon()
                        .appendQueryParameter(SORT_PARAM, params[0])
                       .appendQueryParameter(APPID_PARAM, BuildConfig.MOVIE_DB_API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                movieJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the movie data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getMovieDataFromJson(movieJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<MovieInfo> result) {
            if (result != null) {
                movieListAdapter.clear();
                for(MovieInfo movieInfo : result) {
                    movieListAdapter.add(movieInfo);
                }
            }
        }
    }
}
