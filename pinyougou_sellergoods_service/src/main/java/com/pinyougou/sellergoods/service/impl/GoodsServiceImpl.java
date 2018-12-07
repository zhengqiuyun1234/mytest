package com.pinyougou.sellergoods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.abel533.entity.Example;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pinyougou.mapper.*;
import com.pinyougou.pojo.*;
import com.pinyougou.sellergoods.service.GoodsService;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 业务逻辑实现
 * @author Steven
 *
 */
@Service(interfaceClass = GoodsService.class)
@Transactional
public class GoodsServiceImpl implements GoodsService {

	@Autowired
	private TbGoodsMapper goodsMapper;

	@Autowired
	private TbGoodsDescMapper goodsDescMapper;


	@Autowired
	private TbItemMapper itemMapper;

	@Autowired
	private TbBrandMapper brandMapper;

	@Autowired
	private TbItemCatMapper itemCatMapper;

	@Autowired
	private TbSellerMapper sellerMapper;


	/**
	 * 查询全部
	 */
	@Override
	public List<TbGoods> findAll() {
		return goodsMapper.select(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		
		PageResult<TbGoods> result = new PageResult<TbGoods>();
        //设置分页条件
        PageHelper.startPage(pageNum, pageSize);

        //查询数据
        List<TbGoods> list = goodsMapper.select(null);
        //保存数据列表
        result.setRows(list);

        //获取总记录数
        PageInfo<TbGoods> info = new PageInfo<TbGoods>(list);
        result.setTotal(info.getTotal());
		return result;
	}



    /**
	 * 增加
	 */
	@Override
	public void add(Goods goods) {
		goods.getGoods().setAuditStatus("0");
		goods.getGoods().setIsMarketable("0");
		goodsMapper.insertSelective(goods.getGoods());
		goods.getGoodsDesc().setGoodsId(goods.getGoods().getId());
		goodsDescMapper.insertSelective(goods.getGoodsDesc());

		saveItemList(goods);

	}

	/**
	 * 提取保存sku属性方法
	 * @param goods
	 */
	private void saveItemList(Goods goods) {
		if ("1".equals(goods.getGoods().getIsEnableSpec())){
			for (TbItem tbItem : goods.getItemList()) {
				//设置标题=商品名称+属性名
				String title=goods.getGoods().getGoodsName();

				Map<String,Object> map= JSON.parseObject(tbItem.getSpec());
				for (String s : map.keySet()) {
					title=title+" "+map.get(s);
				}

				tbItem.setTitle(title);
				setItemValue(goods,tbItem );
				itemMapper.insert(tbItem);

			}




		}else {

			TbItem item=new TbItem();
			item.setTitle(goods.getGoods().getGoodsName());//商品KPU+规格描述串作为SKU名称
			item.setPrice( goods.getGoods().getPrice() );//价格
			item.setStatus("1");//状态
			item.setIsDefault("1");//是否默认
			item.setNum(99999);//库存数量
			item.setSpec("{}");
			setItemValue(goods,item);
			itemMapper.insert(item);

		}
	}

	private void setItemValue(Goods goods,TbItem tbItem ){
		//获取图片第一张
		List<Map>imageList=JSON.parseArray(goods.getGoodsDesc().getItemImages(),Map.class);

		if (imageList.size()>0){
			String url = imageList.get(0).get("url").toString();
			tbItem.setImage(url);
		}

		//获取商品类目id

		tbItem.setCategoryid(goods.getGoods().getCategory3Id());

		//获取商品类目内容
		TbItemCat tbItemCat = itemCatMapper.selectByPrimaryKey(tbItem.getCategoryid());
		tbItem.setCategory(tbItemCat.getName());

		//创建日期
		tbItem.setCreateTime(new Date());
		//更新日期
		tbItem.setUpdateTime(tbItem.getCreateTime());

		//设置所属spu id
		tbItem.setGoodsId(goods.getGoods().getId());
		//设置所属商家id
		tbItem.setSellerId(goods.getGoods().getSellerId());
		TbSeller tbSeller = sellerMapper.selectByPrimaryKey(tbItem.getSellerId());
		tbItem.setSeller(tbSeller.getNickName());
		//品牌信息
		Long brandId = goods.getGoods().getBrandId();
		TbBrand tbBrand = brandMapper.selectByPrimaryKey(brandId);
		tbItem.setBrand(tbBrand.getName());



	}


	
	/**
	 * 修改
	 */
	@Override
	public void update(Goods goods){
		goods.getGoods().setAuditStatus("0");//设置状态为未审核
		goodsMapper.updateByPrimaryKeySelective(goods.getGoods());
		goodsDescMapper.updateByPrimaryKeySelective(goods.getGoodsDesc());

		//sku内容先删除后插入新的值
		TbItem where=new TbItem();
		where.setGoodsId(goods.getGoods().getId());
		itemMapper.delete(where);

		saveItemList(goods);
;
	}	
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	@Override
	public Goods findOne(Long id){
		Goods  goods =new Goods();
		TbGoods tbGoods = goodsMapper.selectByPrimaryKey(id);
		goods.setGoods(tbGoods);
		TbGoodsDesc tbGoodsDesc = goodsDescMapper.selectByPrimaryKey(id);
		goods.setGoodsDesc(tbGoodsDesc);

		TbItem where=new TbItem();
		where.setGoodsId(id);
		List<TbItem> tbItems = itemMapper.select(where);
		goods.setItemList(tbItems);


		return  goods;
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		//数组转list
        List longs = Arrays.asList(ids);
        //构建查询条件
        Example example = new Example(TbGoods.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("id", longs);

        //跟据查询条件删除数据
		for (Long id : ids) {
			 TbGoods tbGoods = goodsMapper.selectByPrimaryKey(id);
			tbGoods.setIsDelete("1");
			goodsMapper.updateByExample(tbGoods, example);

		}



	}
	
	
	@Override
	public PageResult findPage(TbGoods goods, int pageNum, int pageSize) {
		PageResult<TbGoods> result = new PageResult<TbGoods>();
        //设置分页条件
        PageHelper.startPage(pageNum, pageSize);

        //构建查询条件
        Example example = new Example(TbGoods.class);
        Example.Criteria criteria = example.createCriteria();
		


			if(goods!=null){
				//如果字段不为空
				if (goods.getSellerId()!=null && goods.getSellerId().length()>0) {
					criteria.andEqualTo("sellerId",goods.getSellerId() );
				}
						//如果字段不为空
			//如果字段不为空
			if (goods.getGoodsName()!=null && goods.getGoodsName().length()>0) {
				criteria.andLike("goodsName", "%" + goods.getGoodsName() + "%");
			}
			//如果字段不为空
			if (goods.getAuditStatus()!=null && goods.getAuditStatus().length()>0) {
				criteria.andLike("auditStatus", "%" + goods.getAuditStatus() + "%");
			}
			//如果字段不为空
			if (goods.getIsMarketable()!=null && goods.getIsMarketable().length()>0) {
				criteria.andLike("isMarketable", "%" + goods.getIsMarketable() + "%");
			}
			//如果字段不为空
			if (goods.getCaption()!=null && goods.getCaption().length()>0) {
				criteria.andLike("caption", "%" + goods.getCaption() + "%");
			}
			//如果字段不为空
			if (goods.getSmallPic()!=null && goods.getSmallPic().length()>0) {
				criteria.andLike("smallPic", "%" + goods.getSmallPic() + "%");
			}
			//如果字段不为空
			if (goods.getIsEnableSpec()!=null && goods.getIsEnableSpec().length()>0) {
				criteria.andLike("isEnableSpec", "%" + goods.getIsEnableSpec() + "%");
			}
			//如果字段不为空
//			if (goods.getIsDelete()!=null && goods.getIsDelete().length()>0) {
//				criteria.andLike("isDelete", "%" + goods.getIsDelete() + "%");
//			}
				//排除已经逻辑删除的数据
				criteria.andIsNull("isDelete");
	
		}

        //查询数据
        List<TbGoods> list = goodsMapper.selectByExample(example);
        //保存数据列表
        result.setRows(list);

        //获取总记录数
        PageInfo<TbGoods> info = new PageInfo<TbGoods>(list);
        result.setTotal(info.getTotal());
		
		return result;
	}

	/**
	 * 商品审核
	 * @param ids
	 * @param status
	 */
	@Override
	public void updateStatus(Long[] ids, String status) {

		TbGoods goods=new TbGoods();
		if ("1".equals(status)){
			goods.setIsMarketable("1");
		}
		goods.setAuditStatus(status);
		List longs = Arrays.asList(ids);

//		Example exampleItem=new Example(TbItem.class);
//		Example.Criteria criteria1 = exampleItem.createCriteria();
//		criteria1.andIn("goodsId", longs);

//		List<TbItem> tbItems = itemMapper.selectByExample(exampleItem);
//		for (TbItem tbItem : tbItems) {
//			tbItem.setStatus(status);
//
//		}
//
//		itemMapper.updateByExample(tbItem,exampleItem);
//


		Example example=new Example(TbGoods.class);
		Example.Criteria criteria = example.createCriteria();

		criteria.andIn("id", longs);
		goodsMapper.updateByExampleSelective(goods, example);



	}

	/**
	 * 商品上下架
	 * @param ids
	 * @param marketable
	 */
    @Override
    public void updateMarketable(Long[] ids,String marketable) {
		TbGoods goods=new TbGoods();
		goods.setIsMarketable(marketable);

		Example example=new Example(TbGoods.class);
		Example.Criteria criteria = example.createCriteria();
		List longs = Arrays.asList(ids);
		criteria.andIn("id", longs);
		goodsMapper.updateByExampleSelective(goods, example);
    }

	/**
	 * 根据选择的id,对比修改的状态  最后查询出审核成功的list集合返回
	 * @param ids  被选中的id
	 * @param status 审核成功的状态
	 * @return
	 */

	@Override
	public List<TbItem> findItemListByGoodsIdsAndStatus(Long[] ids, String status) {
		Example example=new Example(TbItem.class);
		Example.Criteria criteria = example.createCriteria();
		List longs=Arrays.asList(ids);
		criteria.andIn("goodsId",longs );
		criteria.andEqualTo("status", status);

		List<TbItem> items = itemMapper.selectByExample(example);
		return items;
	}


}
