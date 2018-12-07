package com.pinyougou.sellergoods.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.abel533.entity.Example;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pinyougou.mapper.TbItemCatMapper;
import com.pinyougou.pojo.TbItemCat;
import com.pinyougou.sellergoods.service.ItemCatService;
import entity.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * 业务逻辑实现
 * @author Steven
 *
 */
@Service(timeout = 5000)
public class ItemCatServiceImpl implements ItemCatService {

	@Autowired
	private TbItemCatMapper itemCatMapper;

	@Autowired
    private RedisTemplate redisTemplate;
	
	/**
	 * 查询全部
	 */
	@Override
	public List<TbItemCat> findAll() {
		return itemCatMapper.select(null);
	}

	//1根据pid查询所有列表信息
	//2由于增删改都经过这个方法,由此存入缓存最合适
    @Override
    public List<TbItemCat> findByParentId(Long ParentId) {
		TbItemCat tbItemCat=new TbItemCat();
		tbItemCat.setParentId(ParentId);

		List<TbItemCat> tbItemCats = itemCatMapper.select(tbItemCat);

        //查询所有商品类别存入到redis中,以name作为key索引,typeId作为value
        List<TbItemCat> itemCats  = findAll();
        for (TbItemCat itemCat : itemCats) {
            redisTemplate.boundHashOps("itemCat").put(itemCat.getName(),itemCat.getTypeId() );
            
        }

        return tbItemCats;
    }

    /**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		
		PageResult<TbItemCat> result = new PageResult<TbItemCat>();
        //设置分页条件
        PageHelper.startPage(pageNum, pageSize);

        //查询数据
        List<TbItemCat> list = itemCatMapper.select(null);
        //保存数据列表
        result.setRows(list);

        //获取总记录数
        PageInfo<TbItemCat> info = new PageInfo<TbItemCat>(list);
        result.setTotal(info.getTotal());
		return result;
	}

	/**
	 * 增加
	 */
	@Override
	public void add(TbItemCat itemCat) {
		itemCatMapper.insertSelective(itemCat);		
	}

	
	/**
	 * 修改
	 */
	@Override
	public void update(TbItemCat itemCat){
		itemCatMapper.updateByPrimaryKeySelective(itemCat);
	}	
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	@Override
	public TbItemCat findOne(Long id){
		return itemCatMapper.selectByPrimaryKey(id);
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
//		//数组转list
//        List longs = Arrays.asList(ids);


		for (Long id : ids) {
			TbItemCat tbItemCat = itemCatMapper.selectByPrimaryKey(id);


			deleByPid(tbItemCat);
		}




//        //构建查询条件
//        Example example = new Example(TbItemCat.class);
//        Example.Criteria criteria = example.createCriteria();
//        criteria.andIn("id", longs);
//
//        //跟据查询条件删除数据
//        itemCatMapper.deleteByExample(example);
	}


	public void deleByPid(TbItemCat items) {
			List<TbItemCat> byParentId = findByParentId(items.getId());

			if (byParentId.size()!=0){
				for (TbItemCat tbItemCat : byParentId) {
					deleByPid(tbItemCat);
				}

			}else {
			    itemCatMapper.delete(items);
			}
		}




	@Override
	public PageResult findPage(TbItemCat itemCat, int pageNum, int pageSize) {
		PageResult<TbItemCat> result = new PageResult<TbItemCat>();
        //设置分页条件
        PageHelper.startPage(pageNum, pageSize);

        //构建查询条件
        Example example = new Example(TbItemCat.class);
        Example.Criteria criteria = example.createCriteria();
		
		if(itemCat!=null){			
						//如果字段不为空
			if (itemCat.getName()!=null && itemCat.getName().length()>0) {
				criteria.andLike("name", "%" + itemCat.getName() + "%");
			}
	
		}

        //查询数据
        List<TbItemCat> list = itemCatMapper.selectByExample(example);
        //保存数据列表
        result.setRows(list);

        //获取总记录数
        PageInfo<TbItemCat> info = new PageInfo<TbItemCat>(list);
        result.setTotal(info.getTotal());
		
		return result;
	}
	
}
