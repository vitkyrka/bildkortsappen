/*
 * Copyright (c) 2016 Rabin Vincent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package in.rab.bildkort;

import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;

public class ImageFragment extends Fragment {
    static final String GSTATIC_SERVER = "https://encrypted-tbn0.gstatic.com/";

    private WebView mWebView;
    private ArrayList<String> mSelected = new ArrayList<>();
    private String mImagePickerJs;

    public static ImageFragment newInstance(String word) {
        ImageFragment fragment = new ImageFragment();

        Bundle args = new Bundle();
        args.putString("word", word);
        fragment.setArguments(args);

        return fragment;
    }

    private String getWord() {
        return getArguments().getString("word");
    }

    private class WcmJsObject {
        @SuppressWarnings("unused")
        @JavascriptInterface
        public void pushSelected(final String json) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONArray array;
                    try {
                        array = new JSONArray(json);

                        mSelected.clear();

                        for (int i = 0; i < array.length(); i++) {
                            mSelected.add(array.getString(i));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void pushPickerHtml(final String html) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mWebView.loadDataWithBaseURL(GSTATIC_SERVER,
                            html + "<script>" + getImagePickerJs() + "</script>",
                            "text/html", "UTF-8", null);
                }
            });
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mWebView = (WebView) getView().findViewById(R.id.webView);
        final ProgressBar progress = (ProgressBar) getView().findViewById(R.id.image_progress);
        mWebView.setWebChromeClient(new WebChromeClient());
        final WebSettings settings = mWebView.getSettings();

        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptEnabled(true);

        // We replace the src urls in imagepicker.js::init(), so don't load
        // images twice.
        settings.setBlockNetworkImage(true);

        mWebView.setInitialScale(100);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (Objects.equals(url, GSTATIC_SERVER)) {
                    settings.setBlockNetworkImage(false);

                    progress.setVisibility(View.GONE);
                    mWebView.setVisibility(View.VISIBLE);
                } else {
                    view.loadUrl("javascript:" + getImagePickerJs() + "getPickerHtml();");
                }
            }
        });

        mWebView.addJavascriptInterface(new WcmJsObject(), "wcm");
        mWebView.loadUrl("https://www.google.se/search?tbm=isch&q=" + getWord());
    }

    private String getImagePickerJs() {
        if (mImagePickerJs != null) {
            return mImagePickerJs;
        }

        try {
            AssetManager am = getActivity().getAssets();
            InputStream is = am.open("imagepicker.js");
            mImagePickerJs = Utils.inputStreamToString(is);
        } catch (java.io.IOException e) {
            mImagePickerJs = "document.body.innerHtml='Error';";
        }

        return mImagePickerJs;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_image, container, false);
    }

    public ArrayList<String> getSelected() {
        return mSelected;
    }
}
