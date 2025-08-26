package cn.popcraft.model;

/**
 * 过渡类型枚举，定义不同的运镜过渡效果
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
     * 线性过渡
     */
    LINEAR {
        @Override
        public double calculateProgress(double t) {
            return t;
        }
    },
    
    /**
     * 缓入缓出过渡
     */
    EASE_IN_OUT {
        @Override
        public double calculateProgress(double t) {
            return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
        }
    },
    
    /**
     * 缓入过渡
     */
    EASE_IN {
        @Override
        public double calculateProgress(double t) {
            return t * t;
        }
    },
    
    /**
     * 缓出过渡
     */
    EASE_OUT {
        @Override
        public double calculateProgress(double t) {
            return 1 - (1 - t) * (1 - t);
        }
    },
    
    /**
     * 弹跳过渡效果
     */
    BOUNCE {
        @Override
        public double calculateProgress(double t) {
            if (t < 1 / 2.75) {
                return 7.5625 * t * t;
            } else if (t < 2 / 2.75) {
                t -= 1.5 / 2.75;
                return 7.5625 * t * t + 0.75;
            } else if (t < 2.5 / 2.75) {
                t -= 2.25 / 2.75;
                return 7.5625 * t * t + 0.9375;
            } else {
                t -= 2.625 / 2.75;
                return 7.5625 * t * t + 0.984375;
            }
        }
    },
    
    /**
     * 弹性过渡效果
     */
    ELASTIC {
        @Override
        public double calculateProgress(double t) {
            if (t == 0 || t == 1) return t;
            double c4 = 2 * Math.PI / 3;
            return Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * c4) + 1;
        }
    },
    
    /**
     * 平滑过渡（默认）
     */
    SMOOTH {
        @Override
        public double calculateProgress(double t) {
            return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
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