package com.xi.commons.util;

import com.taobao.common.fulllinkstresstesting.StressTestingUtil;
import com.taobao.tddl.client.sequence.Sequence;
import com.taobao.tddl.client.sequence.SequenceDao;
import com.taobao.tddl.client.sequence.exception.SequenceException;
import com.taobao.tddl.client.sequence.impl.DefaultSequence;
import com.taobao.tddl.client.sequence.impl.DefaultSequenceDao;
import com.taobao.tddl.common.model.lifecycle.Lifecycle;

import javax.sql.DataSource;

/**
 * TDDL 默认的 DefaultSequence 是有状态的，里面用一个 volatile 成员变量缓存了当前的 ID 段，在压测时，
 * 压测流量和线上流量都会访问此变量，导致 ID 生成混乱，无法实现影子链路的彻底隔离
 *
 * SmartSequence 可用来代替 DefaultSequence，这个类内部创建了两个 sequence 对象，压测流量和线上流量
 * 分别使用不同的对象，从而保证 ID 生成的完全隔离
 */
public class SmartSequence implements Sequence, Lifecycle {
    private final SequenceDao sequenceDao;
    private final Sequence sequence;
    private final Sequence testSequence;

    public SmartSequence(DataSource dataSource, String name) {
        this.sequenceDao = createSequenceDao(dataSource);
        this.sequence = createSequence(sequenceDao, name);
        this.testSequence = createSequence(sequenceDao, name);
    }

    private SequenceDao createSequenceDao(DataSource dataSource) {
        DefaultSequenceDao sequenceDao = new DefaultSequenceDao();
        sequenceDao.setDataSource(dataSource);
        return sequenceDao;
    }

    private Sequence createSequence(SequenceDao sequenceDao, String name) {
        DefaultSequence sequence = new DefaultSequence();
        sequence.setSequenceDao(sequenceDao);
        sequence.setName(name);
        return sequence;
    }

    @Override
    public long nextValue() throws SequenceException {
        if (StressTestingUtil.isTestFlow()) {
            return testSequence.nextValue();
        } else {
            return sequence.nextValue();
        }
    }

    @Override
    public long nextValue(int size) throws SequenceException {
        if (StressTestingUtil.isTestFlow()) {
            return testSequence.nextValue(size);
        } else {
            return sequence.nextValue(size);
        }
    }

    @Override
    public boolean exhaustValue() throws SequenceException {
        if (StressTestingUtil.isTestFlow()) {
            return testSequence.exhaustValue();
        } else {
            return sequence.exhaustValue();
        }
    }

    @Override
    public void init() {
        sequenceDao.init();
    }

    @Override
    public void destroy() {
        sequenceDao.destroy();
    }

    @Override
    public boolean isInited() {
        return sequenceDao.isInited();
    }
}
