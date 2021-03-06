package com.example.administrator.gc.ui.activity;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.ChangeTransform;
import android.transition.Transition;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.administrator.gc.R;
import com.example.administrator.gc.base.BaseActivity;
import com.example.administrator.gc.model.CommentModel;
import com.example.administrator.gc.presenter.activity.PictureCommentPresenter;
import com.example.administrator.gc.utils.PicassoUtils;
import com.example.administrator.gc.utils.ToastUtils;
import com.example.administrator.gc.widget.CommentDialog;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by liubo on 16/8/1.
 */

public class PictureCommentActivity extends BaseActivity {

    private static final int TYPE_COMMENT = 0x0001;
    private static final int TYPE_LOADING = 0x0002;

    private static final String URL = "url";

    private PictureCommentPresenter presenter;
    private String url;
    private List<CommentModel> viewData = new ArrayList<>();
    private Dialog commentDialog;

    @BindView(R.id.picImageView)
    ImageView picImageView;
    @BindView(R.id.commentRecyclerView)
    RecyclerView commentRecyclerView;

    @BindView(R.id.commentTextView)
    TextView commentTv;

    public static void enter(Activity activity, String url, ImageView view) {
        Intent intent = new Intent(activity, PictureCommentActivity.class);
        intent.putExtra(URL, url);
        int versionId = Build.VERSION.SDK_INT;
        if (versionId >= Build.VERSION_CODES.LOLLIPOP) {
            Transition ts = new ChangeTransform();
            ts.setDuration(300);
            activity.getWindow().setExitTransition(ts);

            Bundle bundle = ActivityOptions.makeSceneTransitionAnimation(activity, view, "shareView").toBundle();
            activity.startActivity(intent, bundle);
        } else {
            activity.startActivity(intent);
        }
    }

    @Override
    protected void initView() {

        setContentView(R.layout.activity_picture_comment);
        ButterKnife.bind(this);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
        } else {
            url = intent.getStringExtra(URL);
        }

        Picasso.with(this)
                .load(url)
                .into(picImageView);

        commentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public void getDataSuccessful(List<CommentModel> list, boolean needClear) {
        if (needClear)
            viewData.clear();
        viewData.addAll(list);
        commentRecyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    protected void setListener() {
        commentRecyclerView.setAdapter(new RVAdapter());

        commentTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cache.readBooleanValue("isLogin", false)) {

                    commentDialog = new CommentDialog(PictureCommentActivity.this, new CommentDialog.OnItemClickListener() {
                        @Override
                        public void onClick(String s) {
                            postComment(s);
                        }
                    });
                    commentDialog.show();

                } else {
                    ToastUtils.showNormalToast("登录后评论.");
                    return;
                }
            }
        });
    }

    @Override
    protected void bind() {
        presenter = new PictureCommentPresenter();
        presenter.bind(this);
        presenter.getComment(url);
    }

    @Override
    protected void unBind() {
        presenter.unBind();
    }

    private class RVAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (TYPE_COMMENT == viewType) {
                return new CommentVh(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false));
            }
            if (TYPE_LOADING == viewType) {
                return new LoadingVH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading_more, parent, false));
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position < viewData.size()) {
                final CommentVh vh = (CommentVh) holder;
                final CommentModel model = viewData.get(position);
                PicassoUtils.normalShowImage(PictureCommentActivity.this, model.getAvatar(), vh.avatar);
                vh.username.setText(model.getName());
                vh.time.setText(new Date(Long.parseLong(model.getTime())) + "");
                vh.admire.setText(String.valueOf(model.getAdmire()));
                vh.commentTv.setText(model.getComment());
                vh.admire.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        admirePic(model.getObjectId(), model);
                        Drawable drawable = getResources().getDrawable(R.mipmap.ic_admired);
                        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
                        vh.admire.setCompoundDrawables(drawable, null, null, null);
                        vh.admire.setText(String.valueOf(model.getAdmire()));
                        vh.admire.setEnabled(false);
                    }
                });
            }
            if (position == viewData.size()) {
                LoadingVH vh = (LoadingVH) holder;
                vh.loadingContent.setVisibility(View.GONE);
                vh.noDataTextView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public int getItemCount() {
            return viewData.size() == 0 ? 1 : viewData.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (viewData.size() == 0 || position == viewData.size()) {
                return TYPE_LOADING;
            } else {
                return TYPE_COMMENT;
            }
        }

        private class CommentVh extends RecyclerView.ViewHolder {
            ImageView avatar;
            TextView username;
            TextView time;
            TextView admire;
            TextView commentTv;


            public CommentVh(View itemView) {
                super(itemView);
                avatar = (ImageView) itemView.findViewById(R.id.avatarImageView);
                username = (TextView) itemView.findViewById(R.id.usernameTextView);
                time = (TextView) itemView.findViewById(R.id.commentTimeTextView);
                admire = (TextView) itemView.findViewById(R.id.admireNoTextView);
                commentTv = (TextView) itemView.findViewById(R.id.commentTextView);
            }
        }

        private class LoadingVH extends RecyclerView.ViewHolder {
            private LinearLayout loadingContent;
            private TextView noDataTextView;

            public LoadingVH(View itemView) {
                super(itemView);
                loadingContent = (LinearLayout) itemView.findViewById(R.id.loadingContent);
                noDataTextView = (TextView) itemView.findViewById(R.id.noDataTextView);
            }
        }
    }

    private void admirePic(String objectId, CommentModel model) {
        int i = model.getAdmire();
        model.setAdmire(i + 1);
        presenter.admirePic(objectId, model);
    }


    private void postComment(String trim) {
        CommentModel model = new CommentModel(String.valueOf(System.currentTimeMillis()), cache.readStringValue("avatar", ""),
                cache.readStringValue("username", ""), 0, trim);
        model.setImageUrl(url);
        presenter.comment(model, url);
    }

    public void commentSuccessful() {
        commentDialog.dismiss();
    }
}
