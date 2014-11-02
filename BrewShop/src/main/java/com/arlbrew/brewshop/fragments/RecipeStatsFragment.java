package com.arlbrew.brewshop.fragments;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.arlbrew.brewshop.R;
import com.arlbrew.brewshop.storage.BrewStorage;
import com.arlbrew.brewshop.storage.NameableAdapter;
import com.arlbrew.brewshop.storage.recipes.BeerStyle;
import com.arlbrew.brewshop.storage.recipes.Recipe;
import com.arlbrew.brewshop.storage.style.BjcpCategory;
import com.arlbrew.brewshop.storage.style.BjcpCategoryList;
import com.arlbrew.brewshop.storage.style.BjcpCategoryStorage;
import com.arlbrew.brewshop.storage.style.BjcpGuidelines;
import com.arlbrew.brewshop.storage.style.BjcpSubcategory;
import com.arlbrew.brewshop.storage.style.CommercialExample;
import com.arlbrew.brewshop.storage.style.VitalStatistics;
import com.arlbrew.brewshop.util.Util;

import java.util.List;

public class RecipeStatsFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    @SuppressWarnings("unused")
    private static final String TAG = RecipeStatsFragment.class.getName();
    private static final String RECIPE = "Recipe";

    private Recipe mRecipe;
    private BrewStorage mStorage;

    private ViewGroup mSubstyleLayout;
    private EditText mRecipeName;
    private Spinner mStyle;
    private Spinner mSubstyle;
    private EditText mBatchVolume;
    private EditText mBoilVolume;
    private EditText mBoilTime;
    private EditText mEfficiency;
    private TextView mDescription;
    private BjcpCategoryList mBjcpCategoryList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View root = inflater.inflate(R.layout.fragment_edit_recipe_stats, container, false);

        setHasOptionsMenu(true);
        mStorage = new BrewStorage(getActivity());

        if (state != null) {
            mRecipe = state.getParcelable(RECIPE);
        }

        if (mRecipe != null) {
            mRecipeName = (EditText) root.findViewById(R.id.recipe_name);
            mRecipeName.setText(mRecipe.getName());

            mDescription = (TextView) root.findViewById(R.id.description);
            mBjcpCategoryList = new BjcpCategoryStorage(getActivity()).getStyles();

            mStyle = (Spinner) root.findViewById(R.id.recipe_style);
            NameableAdapter styleAdapter = new NameableAdapter(getActivity(), mBjcpCategoryList);
            styleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mStyle.setAdapter(styleAdapter);
            mStyle.setOnItemSelectedListener(this);

            mSubstyle = (Spinner) root.findViewById(R.id.recipe_substyle);
            mSubstyle.setOnItemSelectedListener(this);
            setSubstyleList(mBjcpCategoryList.get(0).getSubcategories());

            mSubstyleLayout = (ViewGroup) root.findViewById(R.id.substyle_layout);
            setStyle(mRecipe.getStyle());

            mBatchVolume = (EditText) root.findViewById(R.id.batch_volume);
            mBatchVolume.setText(Util.fromDouble(mRecipe.getBatchVolume(), 5));

            mBoilVolume = (EditText) root.findViewById(R.id.boil_volume);
            mBoilVolume.setText(Util.fromDouble(mRecipe.getBoilVolume(), 5));

            mBoilTime = (EditText) root.findViewById(R.id.boil_time);
            mBoilTime.setText(Util.fromDouble(mRecipe.getBoilTime(), 0));

            mEfficiency = (EditText) root.findViewById(R.id.efficiency);
            mEfficiency.setText(Util.fromDouble(mRecipe.getEfficiency(), 5));
        }
        ActionBar bar = getActivity().getActionBar();
        if (bar != null) {
            bar.setTitle(getActivity().getResources().getString(R.string.edit_recipe_stats));
        }

        return root;
    }

    private void setSubstyleList(List<BjcpSubcategory> subcategories) {
        NameableAdapter substyleAdapter = new NameableAdapter(getActivity(), subcategories);
        substyleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSubstyle.setAdapter(substyleAdapter);
    }

    private void setStyle(BeerStyle style) {
        BjcpCategory category = mBjcpCategoryList.findByName(style.getStyleName());
        int index = mBjcpCategoryList.indexOf(category);
        if (index < 0 ) {
            mStyle.setSelection(0);
            mSubstyle.setSelection(0);
        } else {
            mStyle.setSelection(index);
            setSubstyleList(category.getSubcategories());
            int substyleIdx = category.findSubcategoryIdx(style.getSubstyleName());
            if (substyleIdx < 0 ) {
                mSubstyle.setSelection(0);
            } else {
                mSubstyle.setSelection(substyleIdx);
            }
        }
        mDescription.setText(style.getDescription());
    }

    @Override
    public void onPause() {
        super.onPause();
        String name = mRecipeName.getText().toString();
        if (name.isEmpty()) {
            name = getActivity().getResources().getString(R.string.unnamed_recipe);
        }
        mRecipe.setName(name);
        mRecipe.setBatchVolume(Util.toDouble(mBatchVolume.getText()));
        mRecipe.setBoilVolume(Util.toDouble(mBoilVolume.getText()));
        mRecipe.setBoilTime(Util.toDouble(mBoilTime.getText()));

        double efficiency = Util.toDouble(mEfficiency.getText());
        if (efficiency > 100) {
            efficiency = 100;
        }
        mRecipe.setEfficiency(efficiency);

        BeerStyle style = mRecipe.getStyle();
        VitalStatistics stats = getVitalStatistics();
        style.setOgMin(stats.getOgMin());
        style.setOgMax(stats.getOgMax());
        style.setFgMin(stats.getFgMin());
        style.setFgMax(stats.getFgMax());
        style.setIbuMin(stats.getIbuMin());
        style.setIbuMax(stats.getIbuMax());
        style.setSrmMin(stats.getSrmMin());
        style.setSrmMax(stats.getSrmMax());
        style.setAbvMin(stats.getAbvMin());
        style.setAbvMax(stats.getAbvMax());

        style.setDescription(mDescription.getText().toString());
        mStorage.updateRecipe(mRecipe);
    }

    private VitalStatistics getVitalStatistics() {
        BjcpCategory category = (BjcpCategory) mStyle.getSelectedItem();
        if (category.getSubcategories() == null || category.getSubcategories().isEmpty()) {
            return category.getGuidelines().getVitalStatistics();
        } else {
            BjcpSubcategory subcategory = (BjcpSubcategory) mSubstyle.getSelectedItem();
            return subcategory.getGuidelines().getVitalStatistics();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mStorage.close();
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (state == null) {
            state = new Bundle();
        }
        state.putParcelable(RECIPE, mRecipe);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.edit_recipe_menu, menu);
    }

    public void setRecipe(Recipe recipe) {
        mRecipe = recipe;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        switch (adapterView.getId()) {
            case R.id.recipe_style:
                BjcpCategory category = (BjcpCategory) mStyle.getSelectedItem();
                if (category.getName().equals(mRecipe.getStyle().getStyleName())) {
                    if (category.getSubcategories().isEmpty()) {
                        mSubstyleLayout.setVisibility(View.GONE);
                    }
                } else {
                    mRecipe.getStyle().setStyleName(category.getName());
                    if (category.getSubcategories().isEmpty()) {
                        mRecipe.getStyle().setSubstyleName("");
                        mSubstyleLayout.setVisibility(View.GONE);
                        setDescription();
                    } else {
                        mSubstyleLayout.setVisibility(View.VISIBLE);
                        setSubstyleList(category.getSubcategories());
                        mSubstyle.setSelection(0);
                    }
                }
                break;
            case R.id.recipe_substyle:
                BjcpSubcategory subcategory = (BjcpSubcategory) mSubstyle.getSelectedItem();
                mRecipe.getStyle().setSubstyleName(subcategory.getName());
                setDescription();
                break;
        }
    }

    private void setDescription() {
        BjcpCategory category = (BjcpCategory) mStyle.getSelectedItem();
        StringBuilder description = new StringBuilder();
        description.append(getActivity().getResources().getString(R.string.bjcp_category) + " " + category.getId());

        BjcpGuidelines guidelines = category.getGuidelines();
        List<CommercialExample> examples = category.getCommercialExamples();

        if (category.getSubcategories() != null && !category.getSubcategories().isEmpty()) {
            BjcpSubcategory subcategory = (BjcpSubcategory) mSubstyle.getSelectedItem();
            description.append(subcategory.getLetter());
            guidelines = subcategory.getGuidelines();
            examples = subcategory.getCommercialExamples();
        }

        description.append("\n\n");

        description.append("AROMA" + "\n\n" + guidelines.getAroma() + "\n\n");
        description.append("APPEARANCE" + "\n\n" + guidelines.getAppearance() + "\n\n");
        description.append("FLAVOR" + "\n\n" + guidelines.getFlavor() + "\n\n");
        description.append("MOUTHFEEL" + "\n\n" + guidelines.getMouthfeel() + "\n\n");
        description.append("OVERALL IMPRESSION" + "\n\n" + guidelines.getOverallImpression() + "\n\n");
        description.append("COMMENTS" + "\n\n" + guidelines.getComments() + "\n\n");
        description.append("INGREDIENTS" + "\n\n" + guidelines.getIngredients() + "\n\n");
        description.append("COMMERCIAL EXAMPLES" + "\n\n");

        StringBuilder examplesBuilder = new StringBuilder();
        for (CommercialExample example : examples) {
            examplesBuilder.append("- " + example.getName() + "\n");
        }
        mDescription.setText(Util.separateSentences(description.toString()) + examplesBuilder.toString());
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}