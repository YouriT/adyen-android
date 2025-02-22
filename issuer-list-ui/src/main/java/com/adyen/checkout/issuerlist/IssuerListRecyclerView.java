/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 21/5/2019.
 */

package com.adyen.checkout.issuerlist;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.adyen.checkout.base.ComponentView;
import com.adyen.checkout.base.api.ImageLoader;
import com.adyen.checkout.base.ui.adapter.ClickableListRecyclerAdapter;
import com.adyen.checkout.core.log.LogUtil;
import com.adyen.checkout.core.log.Logger;
import com.adyen.checkout.issuerlist.ui.R;

import java.util.Collections;
import java.util.List;

public abstract class IssuerListRecyclerView<IssuerListComponentT extends IssuerListComponent> extends LinearLayout implements
        ComponentView<IssuerListComponentT>, Observer<List<IssuerModel>>, ClickableListRecyclerAdapter.OnItemCLickedListener {
    private static final String TAG = LogUtil.getTag();

    @Nullable
    protected IssuerListComponentT mComponent;

    private final RecyclerView mIssuersRecyclerView;
    private IssuerListRecyclerAdapter mIssuersAdapter;

    private final IssuerListInputData mIdealInputData = new IssuerListInputData();

    public IssuerListRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public IssuerListRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    // Regular View constructor
    @SuppressWarnings("JavadocMethod")
    public IssuerListRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(getContext()).inflate(R.layout.issuer_list_recycler_view, this, true);

        mIssuersRecyclerView = findViewById(R.id.recycler_issuers);
        mIssuersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @CallSuper
    @Override
    public void attach(@NonNull IssuerListComponentT component, @NonNull LifecycleOwner lifecycleOwner) {
        mComponent = component;

        mIssuersAdapter = new IssuerListRecyclerAdapter(Collections.<IssuerModel>emptyList(),
                ImageLoader.getInstance(getContext(), component.getConfiguration().getEnvironment()), component.getPaymentMethodType(),
                hideIssuersLogo());
        mIssuersAdapter.setItemCLickListener(this);
        mIssuersRecyclerView.setAdapter(mIssuersAdapter);

        mComponent.getIssuersLiveData().observe(lifecycleOwner, this);

        mComponent.sendAnalyticsEvent(getContext());
    }

    @Override
    public boolean isConfirmationRequired() {
        return false;
    }

    public boolean hideIssuersLogo() {
        return false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mIssuersRecyclerView.setEnabled(enabled);
    }

    @Override
    public void onChanged(@Nullable List<IssuerModel> issuerModels) {
        Logger.v(TAG, "onChanged");
        if (issuerModels == null) {
            Logger.e(TAG, "issuerModels is null");
            return;
        }

        mIssuersAdapter.updateIssuerModelList(issuerModels);
    }

    @NonNull
    protected Observer<IssuerListOutputData> createOutputDataObserver() {
        return new Observer<IssuerListOutputData>() {
            @Override
            public void onChanged(@Nullable IssuerListOutputData idealOutputData) {
                // Component does not change selected IssuerModel, add validation UI here if that's needed
            }
        };
    }

    @Override
    public void onItemClicked(int position) {
        Logger.d(TAG, "onItemClicked - " + position);
        mIdealInputData.setSelectedIssuer(mIssuersAdapter.getIssuerAt(position));
        if (mComponent != null) {
            mComponent.inputDataChanged(mIdealInputData);
        } else {
            Logger.e(TAG, "component null");
        }
    }
}
