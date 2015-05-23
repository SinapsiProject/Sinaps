package com.sinapsi.android.view;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.pkmmte.view.CircularImageView;
import com.sinapsi.android.Lol;
import com.sinapsi.android.background.SinapsiFragment;
import com.sinapsi.android.background.SinapsiBackgroundService;
import com.sinapsi.android.background.WebServiceConnectionListener;
import com.sinapsi.android.utils.ArrayListAdapter;
import com.sinapsi.android.utils.ViewTransitionManager;
import com.sinapsi.android.utils.swipeaction.SwipeActionLayoutManager;
import com.sinapsi.android.utils.swipeaction.SwipeActionMacroExampleButton;
import com.sinapsi.engine.R;
import com.sinapsi.model.MacroInterface;
import com.sinapsi.model.impl.FactoryModel;
import com.sinapsi.model.impl.Macro;
import com.sinapsi.utils.HashMapBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The main macro manager fragment.
 */
public class MacroManagerFragment extends SinapsiFragment implements WebServiceConnectionListener {



    private ViewTransitionManager transitionManager;
    private ArrayListAdapter<MacroInterface> macroList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean created = false;

    private enum States {
        NO_ELEMENTS,
        NO_CONNECTION,
        LIST,
        PROGRESS
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_macro_manager, container, false);



        FloatingActionButton fab =(FloatingActionButton) rootView.findViewById(R.id.new_macro_button);
        RecyclerView macroListRecycler = (RecyclerView) rootView.findViewById(R.id.macro_list_recycler);

        swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
        Lol.printNullity(this, "swipeRefreshLayout", swipeRefreshLayout);

        transitionManager = new ViewTransitionManager(new HashMapBuilder<String, List<View>>()
                .put(States.NO_ELEMENTS.name(), Arrays.asList(
                        rootView.findViewById(R.id.no_macros_text), fab))
                .put(States.NO_CONNECTION.name(), Collections.singletonList(
                        rootView.findViewById(R.id.no_connection_layout)))
                .put(States.LIST.name(), Arrays.asList(
                        macroListRecycler, fab))
                .put(States.PROGRESS.name(), Collections.singletonList(
                        rootView.findViewById(R.id.macro_list_progress)))
                .create());


        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        macroListRecycler.setLayoutManager(layoutManager);
        macroListRecycler.setHasFixedSize(true);

        macroList = new ArrayListAdapter<MacroInterface>() {
            @Override
            public View onCreateView(ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.macro_manager_element, parent, false);
                final SwipeLayout sl = (SwipeLayout) v.findViewById(R.id.macro_element_swipe_layout);
                final ImageButton button = (ImageButton) v.findViewById(R.id.show_more_macro_actions_button);
                final Animation rotation = AnimationUtils.loadAnimation(getActivity(), R.anim.button_rotate);
                rotation.setRepeatCount(Animation.INFINITE);
                View.OnClickListener openCloseListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(sl.getOpenStatus() == SwipeLayout.Status.Open)
                            sl.close(true);
                        else if(sl.getOpenStatus() == SwipeLayout.Status.Close)
                            sl.open(true);
                    }
                };

                button.setOnClickListener(openCloseListener);


                sl.setShowMode(SwipeLayout.ShowMode.PullOut);
                sl.addSwipeListener(new SwipeLayout.SwipeListener() {
                    @Override
                    public void onStartOpen(SwipeLayout swipeLayout) {
                        button.startAnimation(rotation);
                        swipeRefreshLayout.setEnabled(false);
                    }

                    @Override
                    public void onOpen(SwipeLayout swipeLayout) {
                        //swipeRefreshLayout.setEnabled(true);
                    }

                    @Override
                    public void onStartClose(SwipeLayout swipeLayout) {
                        swipeRefreshLayout.setEnabled(false);
                    }

                    @Override
                    public void onClose(SwipeLayout swipeLayout) {
                        swipeRefreshLayout.setEnabled(true);
                        button.clearAnimation();
                    }

                    @Override
                    public void onUpdate(SwipeLayout swipeLayout, int i, int i2) {

                    }

                    @Override
                    public void onHandRelease(SwipeLayout swipeLayout, float v, float v2) {
                        swipeRefreshLayout.setEnabled(true);
                    }
                });

                TextView eventTitle = (TextView) v.findViewById(R.id.macro_title);
                eventTitle.setSelected(true);

                return v;
            }

            @Override
            public void onBindViewHolder(ItemViewHolder viewHolder, MacroInterface elem) {
                View v = viewHolder.itemView;

                Lol.d(ArrayListAdapter.class, elem.getName() + " just binded to a viewHolder");
                ((TextView) v.findViewById(R.id.macro_title)).setText(elem.getName());
                //TODO: set description and other data

                LinearLayout ll = (LinearLayout) v.findViewById(R.id.bottom_wrapper);
                SwipeActionLayoutManager salm = new SwipeActionLayoutManager(getActivity(), ll);

                //TODO: put real context actions here
                salm.addAction(new SwipeActionMacroExampleButton(elem, getActivity()));
                salm.addAction(new SwipeActionMacroExampleButton(elem, getActivity()));

                CircularImageView ciw = (CircularImageView) v.findViewById(R.id.macro_element_icon);

                int resourceId = getResources().getIdentifier(elem.getIconName(), "drawable", v.getContext().getPackageName());
                if(resourceId == 0)
                    ciw.setImageResource(R.drawable.ic_macro_default);
                else
                    ciw.setImageResource(resourceId);

                //ciw.setBackgroundColor(Color.parseColor(elem.getMacroColor()));
            }
        };

        macroListRecycler.setAdapter(macroList);

        macroListRecycler.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                boolean enable = false;
                if (recyclerView != null && recyclerView.getChildCount() > 0) {
                    // check if the first item of the list is visible
                    boolean firstItemVisible = layoutManager.findFirstCompletelyVisibleItemPosition() == 0;
                    // check if the top of the first item is visible
                    boolean topOfFirstItemVisible = recyclerView.getChildAt(0).getTop() == 0;
                    // enabling or disabling the refresh layout
                    enable = firstItemVisible && topOfFirstItemVisible;
                }
                swipeRefreshLayout.setEnabled(enable);
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newMacro();
            }
        });

        FloatingActionButton retryButton = (FloatingActionButton) rootView.findViewById(R.id.retry_button);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateContent();
            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateContent();
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.sinapsi_blue);
        transitionManager.makeTransitionIfDifferent(States.PROGRESS.name());

        created = true;
        updateContent();
        return rootView;
    }

    private void updateContent() {
        Lol.d(this, "Update content started");
        if(!isServiceConnected()) return;
        swipeRefreshLayout.setRefreshing(true);
        transitionManager.makeTransitionIfDifferent(States.PROGRESS.name());



        //TODO: handle server sync
        //updateMacroList(service.getMacros());
        //TODO: remove and decomment the line above, this is just for test
        updateMacroList(Arrays.asList(
                new FactoryModel().newMacro("Macro 1", 1),
                new FactoryModel().newMacro("Macro 2", 2)
        ));

        Lol.d(this, "Macro showed: " + macroList.getItemCount());

        swipeRefreshLayout.setRefreshing(false);
        transitionManager.makeTransitionIfDifferent(States.LIST.name());
        Lol.d(this, "Update content finished");
    }

    private void newMacro() {
        Lol.d(this, "newMacro called");
        //TODO: impl
    }

    @Override
    public void onOnlineMode() {
        //TODO: impl
    }

    @Override
    public void onOfflineMode() {
        //TODO: impl
    }

    private void updateMacroList(List<MacroInterface> ml) {
        macroList.clear();
        macroList.addAll(ml);
    }

    @Override
    public String getName(Context context) {
        return context.getString(R.string.macro_manager_fragment_title);
    }

    @Override
    public void onServiceConnected(SinapsiBackgroundService service) {
        super.onServiceConnected(service);
        service.addWebServiceConnectionListener(this);

        //updates on service connected only if this is visible to the user
        if(created)updateContent();
    }
}
