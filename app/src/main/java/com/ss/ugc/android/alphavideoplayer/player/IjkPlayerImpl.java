package com.ss.ugc.android.alphavideoplayer.player;

import android.media.AudioManager;
import android.util.Log;
import android.view.Surface;

import com.blankj.utilcode.util.ThreadUtils;
import com.ss.ugc.android.alpha_player.model.VideoInfo;
import com.ss.ugc.android.alpha_player.player.AbsPlayer;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * @Description:
 * @Author: CJ
 * @CreateDate: 2023/10/13 22:14
 */
public class IjkPlayerImpl extends AbsPlayer {

    private static final String TAG = "IjkPlayerImpl";
    private Surface mSurface;

    private IjkMediaPlayer mIjkMediaPlayer;

    @Override
    public void initMediaPlayer() throws Exception {
        mIjkMediaPlayer = new IjkMediaPlayer();
        mIjkMediaPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer iMediaPlayer) {
                if (getPreparedListener() != null) {
                    getPreparedListener().onPrepared();
                }
            }
        });
        mIjkMediaPlayer.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(IMediaPlayer iMediaPlayer) {
                if (getCompletionListener() != null) {
                    getCompletionListener().onCompletion();
                }
            }
        });
        mIjkMediaPlayer.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer iMediaPlayer, int i, int i1) {
                if (getErrorListener() != null) {
                    getErrorListener().onError(i, i1, "ijk error");
                }
                return false;
            }
        });
        mIjkMediaPlayer.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
                if (i == IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START){
                    ThreadUtils.runOnUiThreadDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (getFirstFrameListener() != null) {
                                getFirstFrameListener().onFirstFrame();
                            }
                        }
                    }, 50);
                }
                return false;
            }
        });
        //native日志
        boolean isOpenLog = false;
        IjkMediaPlayer.native_setLogLevel(isOpenLog ? IjkMediaPlayer.IJK_LOG_INFO : IjkMediaPlayer.IJK_LOG_SILENT);
        setOptions(mIjkMediaPlayer);
    }

    @Override
    public void setSurface(@NotNull Surface surface) {
        mSurface = surface;
//        mIjkMediaPlayer.setSurface(surface);
    }

    @Override
    public void setDataSource(@NotNull String dataPath) throws IOException {
        mIjkMediaPlayer.setDataSource(dataPath);
    }

    @Override
    public void prepareAsync() {
        mIjkMediaPlayer.prepareAsync();
    }

    @Override
    public void start() {
        mIjkMediaPlayer.setSurface(mSurface);
        mIjkMediaPlayer.start();
    }

    @Override
    public void pause() {
        mIjkMediaPlayer.pause();
    }

    @Override
    public void stop() {
        mIjkMediaPlayer.stop();
    }

    @Override
    public void reset() {
        mIjkMediaPlayer.reset();
        setOptions(mIjkMediaPlayer);
    }

    @Override
    public void release() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mIjkMediaPlayer.release();
            }
        }).start();
    }

    @Override
    public void setLooping(boolean looping) {
        mIjkMediaPlayer.setLooping(looping);
    }

    @Override
    public void setScreenOnWhilePlaying(boolean onWhilePlaying) {

    }

    @NotNull
    @Override
    public VideoInfo getVideoInfo() throws Exception {
        return new VideoInfo(mIjkMediaPlayer.getVideoWidth(), mIjkMediaPlayer.getVideoHeight());
    }

    @NotNull
    @Override
    public String getPlayerType() {
        return "ijkPlayerImpl";
    }

    /**
     * option 优化项
     *
     * @param ijkMediaPlayer
     */
    private void setOptions(IjkMediaPlayer ijkMediaPlayer) {
        ijkMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        //1为硬解 0为软解
        int mediaCodec = 1;
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", mediaCodec);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-all-videos", mediaCodec);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-avc", mediaCodec);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-mpeg2", mediaCodec);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-mpeg4", mediaCodec);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-hevc", mediaCodec);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", mediaCodec);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", mediaCodec);
        //使用opensles把文件从java层拷贝到native层
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
        //视频格式
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
        //跳帧处理（-1~120）。CPU处理慢时，进行跳帧处理，保证音视频同步
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
        //1为一进入就播放,0为进入时不播放
        setOnPrepared(ijkMediaPlayer, true);
        //域名检测
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
        //设置是否开启环路过滤: 0开启，画面质量高，解码开销大，48关闭，画面质量差点，解码开销小
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
        //最大缓冲大小,单位kb
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-buffer-size", 1024 * 1024);
        //某些视频在SeekTo的时候，会跳回到拖动前的位置，这是因为视频的关键帧的问题，通俗一点就是FFMPEG不兼容，视频压缩过于厉害，seek只支持关键帧，出现这个情况就是原始的视频文件中i 帧比较少
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1);
        //是否重连
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1);
        //http重定向https
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1);
        //设置seekTo能够快速seek到指定位置并播放
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "fflags", "fastseek");
        //播放前的探测Size，默认是1M, 改小一点会出画面更快
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", 1024);
        //1变速变调状态 0变速不变调状态
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "soundtouch", 1);
    }

    public void setOnPrepared(IjkMediaPlayer ijkMediaPlayer, boolean isStart) {
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", isStart ? 1 : 0);
    }
}
