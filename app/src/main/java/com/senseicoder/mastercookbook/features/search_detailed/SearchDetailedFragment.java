package com.senseicoder.mastercookbook.features.search_detailed;


import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.senseicoder.mastercookbook.R;
import com.senseicoder.mastercookbook.db.local.RoomLocalDateSourceImpl;
import com.senseicoder.mastercookbook.db.remote.FirebaseFirestoreRemoteDataSourceImpl;
import com.senseicoder.mastercookbook.features.search_detailed.adapters.SearchDetailedAdapter;
import com.senseicoder.mastercookbook.features.search_detailed.listeners.SearchDetailedRecyclerListeners;
import com.senseicoder.mastercookbook.features.search_detailed.presenter.SearchDetailedPresenter;
import com.senseicoder.mastercookbook.features.search_detailed.presenter.SearchDetailedPresenterImpl;
import com.senseicoder.mastercookbook.features.search_detailed.view.SearchDetailedView;
import com.senseicoder.mastercookbook.model.DTOs.MealSimplifiedModel;
import com.senseicoder.mastercookbook.model.MealKeys;
import com.senseicoder.mastercookbook.model.repositories.DataRepositoryImpl;
import com.senseicoder.mastercookbook.network.RetrofitRemoteDataSourceImpl;
import com.senseicoder.mastercookbook.util.enums.SearchType;
import com.senseicoder.mastercookbook.util.global.UiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;


public class SearchDetailedFragment extends Fragment implements SearchDetailedView, SearchDetailedRecyclerListeners {

    private static final String TAG = "SearchDetailedFragment";

    RecyclerView searchDetailedRecyclerView;
    SearchView searchView;
    ProgressBar searchDetailedProgressBar;
    Group searchDetailedGroup;
    ListView suggestionListView;
    TextView mealDoesntExitTextView;

    SearchDetailedAdapter adapter;
    SearchDetailedPresenter presenter;
    ArrayAdapter<String> suggestionAdapter;
    SearchType searchType;

    Observable<String> textObservable;

    String currentText;
    List<String> suggestions;
    List<MealSimplifiedModel> meals;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        searchType = SearchDetailedFragmentArgs.fromBundle(getArguments()).getSearchType();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_detailed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchDetailedRecyclerView = view.findViewById(R.id.searchDetailedRecyclerView);
        searchView = view.findViewById(R.id.searchView);
        searchDetailedProgressBar = view.findViewById(R.id.searchDetailedProgressBar);
        searchDetailedGroup = view.findViewById(R.id.searchDetailedGroup);
        suggestionListView = getView().findViewById(R.id.suggestionsListView);
        mealDoesntExitTextView = getView().findViewById(R.id.mealDoesntExitTextView);

        searchView.setQueryHint(setQueryHint(searchType));
        adapter = new SearchDetailedAdapter(new ArrayList<>(), getContext(), this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        searchDetailedRecyclerView.setLayoutManager(layoutManager);
        searchDetailedRecyclerView.setAdapter(adapter);

        presenter = new SearchDetailedPresenterImpl(
                DataRepositoryImpl.getInstance(
                        FirebaseFirestoreRemoteDataSourceImpl.getInstance(),
                        RetrofitRemoteDataSourceImpl.getInstance(getActivity().getCacheDir()),
                        RoomLocalDateSourceImpl.getInstance(getContext())
                ),
                this
        );

        searchView.clearFocus();
        if(meals == null)
            presenter.getListByType(searchType);
        else{
            presenter.reloadState(meals, searchType, suggestions);
        }
    }

    @Override
    public void updateMealsList(List<MealSimplifiedModel> meals) {
        Log.d(TAG, "updateMealsList: " + meals.size());
        suggestionListView.setVisibility(View.GONE);
        if(meals.isEmpty())
            mealDoesntExitTextView.setVisibility(View.VISIBLE);
        this.meals = meals;
        adapter.updateList(meals);
    }

    @Override
    public void updateSearchList(List<String> suggestions) {
        setupListeners();
        suggestionAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, suggestions);
        this.suggestions = suggestions;
        suggestionListView.setAdapter(suggestionAdapter);
        searchDetailedProgressBar.setVisibility(View.GONE);
        searchDetailedGroup.setVisibility(View.VISIBLE);
    }

    @Override
    public void handleError(Throwable throwable) {
        Log.d(TAG, "handleError: " + throwable.getMessage());
        throwable.printStackTrace();
        if (!(throwable instanceof NullPointerException)) {
            Toast.makeText(getContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void initTextObservable() {
        searchDetailedProgressBar.setVisibility(View.GONE);
        searchDetailedGroup.setVisibility(View.VISIBLE);
        suggestionListView.setVisibility(View.GONE);
        textObservable = Observable.create( emitter -> {
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    UiUtils.hideKeyboardFrom(getContext(), getView());
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    Log.d(TAG, "onQueryTextChange: " + newText);
                    mealDoesntExitTextView.setVisibility(View.INVISIBLE);
                    if(newText!= null && !newText.isEmpty()){
                        emitter.onNext(newText);
                    }
                    return true;
                }
            });
            }
        );
        presenter.addDisposable(textObservable.distinctUntilChanged().debounce(500, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> presenter.getMealsBySearchWord(s, SearchType.Name)));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.clear();
    }

    private void setupListeners() {
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                suggestionListView.setVisibility(View.VISIBLE);
            else {
                suggestionListView.setVisibility(View.GONE);
            }
        });
        suggestionListView.setOnItemClickListener((parent, v, position, id) -> {
            String suggestion = (String) parent.getItemAtPosition(position);
            searchView.setQuery(suggestion, true);
            Log.d(TAG, "onItemClick: " + suggestion);
            searchView.clearFocus();
            suggestionListView.setVisibility(View.GONE);
            if(currentText == null || !currentText.equals(suggestion)){
                Log.d(TAG, "onItemClick: working");
                presenter.getMealsBySearchWord(suggestion, searchType);
                currentText = suggestion;
            }

        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                suggestionListView.setVisibility(View.GONE);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "onQueryTextChange: " + newText);
                suggestionListView.setVisibility(View.VISIBLE);
                suggestionAdapter.getFilter().filter(newText);
                return true;
            }
        });
    }


    private String setQueryHint(SearchType searchType){
        String queryHintText = null;
        switch(searchType){
            case Name:
                queryHintText = getString(R.string.search_detailed_meal_hint);
                break;
            case Country:
                queryHintText = getString(R.string.search_detailed_country_hint);
                break;
            case Ingredient:
                queryHintText = getString(R.string.search_detailed_ingredient_hint);
                break;
            case Category:
                queryHintText = getString(R.string.search_detailed_category_hint);
                break;

        }
        return queryHintText;
    }

    @Override
    public void onItemClicked(String mealId) {
        SearchDetailedFragmentDirections.ActionSearchDetailedFragmentToMealFragment action =
                SearchDetailedFragmentDirections.actionSearchDetailedFragmentToMealFragment(mealId);
        Navigation.findNavController(getView()).navigate(action);
    }
}