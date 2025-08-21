package cn.popcraft.model;

/**
 * 过渡类型枚举
 */
public enum TransitionType {
    /**
     * 无过渡 - 直接切换到目标位置
     */
    NONE {
        @Override
        public double calculateProgress(double t) {
            return 1.0; // 立即完成
        }
    },
    
    /**
     * 线性过渡 - 匀速移动到目标位置
     */
    LINEAR {
        @Override
        public double calculateProgress(double t) {
            return t;
        }
    },
    
    /**
     * 缓入过渡 - 开始慢，然后加速
     */
    EASE_IN {
        @Override
        public double calculateProgress(double t) {
            return t * t; // 二次方缓入
        }
    },
    
    /**
     * 缓出过渡 - 开始快，然后减速
     */
    EASE_OUT {
        @Override
        public double calculateProgress(double t) {
            return 1 - (1 - t) * (1 - t); // 二次方缓出
        }
    },
    
    /**
     * 缓入缓出过渡 - 开始慢，中间快，结束慢
     */
    EASE_IN_OUT {
        @Override
        public double calculateProgress(double t) {
            return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2; // 二次方缓入缓出
        }
    };

    /**
     * 计算过渡进度
     * @param t 原始进度，范围[0,1]
     * @return 调整后的进度，范围[0,1]
     */
    public abstract double calculateProgress(double t);

    /**
     * 计算两点之间的插值
     * @param start 起始值
     * @param end 结束值
     * @param t 进度，范围[0,1]
     * @return 插值结果
     */
    public static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    /**
     * 计算两个角度之间的插值（考虑角度循环）
     * @param start 起始角度
     * @param end 结束角度
     * @param t 进度，范围[0,1]
     * @return 插值结果
     */
    public static float lerpAngle(float start, float end, double t) {
        // 确保角度在[-180, 180]范围内
        float shortestAngle = ((((end - start) % 360) + 540) % 360) - 180;
        return (float) (start + shortestAngle * t);
    }
}