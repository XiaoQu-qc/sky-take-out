package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishPageQueryDTO;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.BaseException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增套餐和对应的菜品
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {

        //新增套餐
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);

        //套餐新增完成后才有套餐id，获取套餐id
        Long setmealId = setmeal.getId();

        //新增相应的菜品，即套餐菜品的对应关系
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && setmealDishes.size() > 0) {
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealId);
            });
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }

    /**
     * 分页查询套餐数据
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        PageResult pageResult = new PageResult(page.getTotal(),page.getResult());
        return pageResult;
    }

    /**
     * 根据传入的id批量删除套餐
     * @param ids
     */
    @Transactional
    @Override
    public void deleteBatch(List<Long> ids) {

        if(ids == null || ids.size() == 0) {
            throw new BaseException(MessageConstant.UNKNOWN_ERROR);
        }
        //如果存在启售状态的套餐则不能删除，抛出异常由自定义的全局异常处理器处理
        for(Long id : ids) {
            Setmeal setmeal = setmealMapper.getById(id);
            if(setmeal.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        //删除setmeal表中数据
        setmealMapper.deleteByIds(ids);

        //删除setmealdish表中数据
        setmealDishMapper.deleteBySetmealIds(ids);
    }

    /**
     * 根据套餐id查询套餐(套餐所含菜品)
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {

        SetmealVO setmealVO = new SetmealVO();

        //根据id查询套餐
        Setmeal setmeal = setmealMapper.getById(id);
        BeanUtils.copyProperties(setmeal, setmealVO);

        //根据套餐id查询相应的菜品
        //TODO categoryName字段没有数据
        List<SetmealDish> setmealDishes= setmealDishMapper.getBySetmealId(id);

        //两个返回结果并入SetmealVO，当然也可以在数据库表中连表查询（又好像不行？）
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    /**
     * 修改套餐信息和套餐包含的菜品信息
     * @param setmealDTO
     */
    @Transactional
    @Override
    public void updateWithDish(SetmealDTO setmealDTO) {

        //修改套餐信息，setmeal表
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        //删除当前要修改的套餐的菜品信息setmeal_dish表
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());


        //修改套餐包含的菜品信息，setmeal_dish表
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if(setmealDishes != null && setmealDishes.size() > 0) {
            for(SetmealDish setmealDish : setmealDishes) {
                setmealDish.setSetmealId(setmealDTO.getId());
            }
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        Setmeal setmeal = new Setmeal().builder()
                .status(status)
                .id(id)
                .build();
        setmealMapper.update(setmeal);
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }
}
