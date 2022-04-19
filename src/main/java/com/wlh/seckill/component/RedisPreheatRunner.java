package com.wlh.seckill.component;

import com.wlh.seckill.db.dao.SeckillActivityDao;
import com.wlh.seckill.db.po.SeckillActivity;
import com.wlh.seckill.util.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisPreheatRunner  implements ApplicationRunner {
    @Autowired
    RedisService redisService;

    @Autowired
    SeckillActivityDao seckillActivityDao;

    /**
     * 啟動項目時 向 Redis 存入 商品庫存
     * Introduce inventory to Redis when starting the project
     * @param args
     * @throws Exception
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<SeckillActivity> seckillActivities = seckillActivityDao.querySeckillActivitysByStatus(1);
        for (SeckillActivity seckillActivity : seckillActivities) {
            redisService.setValue("stock:" + seckillActivity.getId(),
                    (long) seckillActivity.getAvailableStock());
        }
    }
}

