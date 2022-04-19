package com.wlh.seckill.web;


import com.wlh.seckill.services.SeckillActivityService;
import com.wlh.seckill.services.SeckillOverSellService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SeckillOverSellController {

    @Autowired
    private SeckillOverSellService seckillOverSellService;


    @Autowired
    private SeckillActivityService seckillActivityService;

    /**
     * 处理抢购请求
     * Handling purchase requests
     * @param seckillActivityId
     * @return
     */
    @ResponseBody
//    @RequestMapping("/seckill/{seckillActivityId}")
    public String  seckil(@PathVariable long seckillActivityId){
        return seckillOverSellService.processSeckill(seckillActivityId);
    }

    /**
     * 使用 lua 脚本处理抢购请求
     * Handle purchase requests with lua scripts
     * @param seckillActivityId
     * @return
     */
    @ResponseBody
    @RequestMapping("/seckill/{seckillActivityId}")
    public String seckillCommodity(@PathVariable long seckillActivityId) {
        boolean stockValidateResult = seckillActivityService.seckillStockValidator(seckillActivityId);
        // "Congratulations on your success" : "Items are sold out, come back next time."
        return stockValidateResult ? "恭喜你秒杀成功" : "商品已经售完，下次再来";
    }

}


