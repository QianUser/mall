package com.example.mall.cart.service;

import com.example.mall.cart.vo.CartItemVo;
import com.example.mall.cart.vo.CartVo;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CartService {

    /**
     * 将商品添加至购物车
     */
    CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    /**
     * 获取购物车某个购物项
     */
    CartItemVo getCartItem(Long skuId);

    /**
     * 获取购物车里面的信息
     */
    CartVo getCart() throws ExecutionException, InterruptedException;

    /**
     * 清空购物车的数据
     */
    public void clearCartInfo(String cartKey);

    /**
     * 勾选购物项
     */
    void checkItem(Long skuId, Integer check);

    /**
     * 改变商品数量
     */
    void changeItemCount(Long skuId, Integer num);


    /**
     * 删除购物项
     */
    void deleteIdCartInfo(Integer skuId);

    List<CartItemVo> getUserCartItems();

}
