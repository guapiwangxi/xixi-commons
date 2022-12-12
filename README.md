# xi-cache
一个支持guava和redis等支撑多租户业务的多级缓存实现，有良好的抽象，并可以扩展其他缓存框架实现。


## 主要概念
首先，我们来了解一下主要的概念：

* ICacheManager: 缓存管理器主接口，实现类是 MultiLevelCacheManager，支持二级缓存
* ICacheClient: 缓存客户端接口，框架通过此接口来集成各种缓存客户端，包含 Guava、Tair、Redis 等实现
* CacheConfig: 缓存配置类，应用中的所有缓存都应该拥有一个缓存配置，包含以下字段
  * name: 缓存名<br/>
  * version: 版本号<br/>
  * expireTime: 缓存过期时间，从数据写入时开始计时，时间达到后自动删除数据<br/>
  * reloadTime: 缓存重加载时间，从数据写入时开始计时，时间达到后触发异步重加载<br/>
  * level1: 一级缓存使用的客户端类型<br/>
  * level2: 二级缓存使用的客户端类型<br/>


## 配置缓存管理器
使用之前，我们首先需要创建一个缓存管理器对象，通常我们会把它注册为 Spring 容器中的 bean，例如下面这段代码，我们创建了一个缓存管理器，并配置了两个缓存：

```js
@Configuration
public class CacheConfiguration {

    @Autowired
    private TairManager tairManager;
    
    @Value("${spring.tair.namespace}")
    private int namespace;
    
    @Bean
    public ICacheManager multiLevelCacheManager() {
        ICacheManager cacheManager = new MultiLevelCacheManager();
        // 添加 Guava 缓存客户端
        cacheManager.addClient(new GuavaCacheClient());
        // 添加 Tair 缓存客户端
        cacheManager.addClient(new TairCacheClient(tairManager, namespace));
        
        // 配置 TASK_TEMPLATE 缓存，使用二级缓存、支持自动 reload
        cacheManager.addConfig(
            CacheConfig.builder()
                .name(CacheNames.TASK_TEMPLATE)
                .expireTime(Duration.ofMinutes(5))
                .reloadTime(Duration.ofMinutes(4))
                .level1(CacheType.GUAVA)
                .level2(CacheType.TAIR)
                .build());
        
        // 配置 TASK_BY_ID 缓存，使用一级缓存
        cacheManager.addConfig(
            CacheConfig.builder()
                .name(CacheNames.TASK_BY_ID)
                .expireTime(Duration.ofMinutes(10))
                .level1(CacheType.TAIR)
                .build());
        
        // 初始化缓存管理器
        cacheManager.initialize();
        return cacheManager;
    }
}
```

## 使用 Kotlin 配置缓存管理器
在上面的例子中，我们可以看见，缓存的配置代码十分冗长，写起来非常麻烦。为解决这个问题，传统的 Java 框架一般会设计一个 xml 配置文件，我们只需要在代码中读取这个配置文件即可
但是我们使用 Kotlin，这可以帮我们使用极少的代码量实现简洁优雅并且类型安全的配置逻辑，例如这段代码实现了相同的功能，看起来却比前面的 Java 代码精简许多：
```js
@Configuration
class CacheConfiguration {
    @Autowired
    lateinit var tairManagerWrapper: TairManagerWrapper
    
    @Bean
    fun multiLevelCacheManager(): ICacheManager = multiLevelCacheManager {
        clients {
            guava()
            landlordTair(tairManagerWrapper)
        }
        caches {
            cache(TASK_TEMPLATE, expire = 5.min, reload = 4.min, l1 = GUAVA, l2 = TAIR)
            cache(TASK_BY_ID, expire = 10.min, l1 = TAIR)
            cache(CURRENT_TASK, expire = 10.min, l1 = TAIR)
        }
    }
}
```

## 开始使用
配置完毕后，我们只需要注入 ICacheManager 对象，就可以使用了，下面举几个例子。

从缓存中获取数据，未命中时，调用 loader 加载：
```js
TaskTemplateDTO template = cacheManager.get(TASK_TEMPLATE, taskCode, k -> {
    return taskTemplateDao.getTaskTemplateByCode(taskCode);
});
```

从缓存中批量获取数据，针对未命中的 key，调用 loader 加载：
```js
Map<String, TaskTemplateDTO> templates = cacheManager.getAll(TASK_TEMPLATE, taskCodes, keys -> {
    List<TaskTemplateDTO> list = taskTemplateDao.getTaskTemplateByCodes(keys);
    if (list == null) {
        return Collections.emptyMap();
    } else {
        return CollectionsKt.associateBy(list, TaskTemplateDTO::getTaskCode);
    }
});
```
框架默认会缓存 loader 返回的所有数据（包括 null 值），在某些特殊情况下，如果我们不希望缓存 null 值，可以通过覆盖 loader 的 shouldCache 方法来实现：
```js
TaskTemplateDTO template = cacheManager.get(TASK_TEMPLATE, taskCode, new ICacheLoader<String, TaskTemplateDTO>() {
    @Override
    public TaskTemplateDTO load(String key) {
        return taskTemplateDao.getTaskTemplateByCode(key);
    }

    @Override
    public boolean shouldCache(TaskTemplateDTO value) {
        return value != null;
    }
});
```
## 支持 Spring 注解
Spring 提供了一套缓存标准注解，在某些情况下使用注解具有不可替代的优势。因此我们也支持与 Spring 框架进行集成，只需要使用 asSpringCacheManager 方法进行适配即可，例如：
```js
@EnableCaching
@Configuration
class CacheConfiguration : CachingConfigurerSupport() {
    @Autowired
    lateinit var tairManagerWrapper: TairManagerWrapper
    
    @Bean
    override fun cacheManager(): CacheManager {
        return multiLevelCacheManager().asSpringCacheManager()
    }
    
    @Bean
    fun multiLevelCacheManager(): ICacheManager = multiLevelCacheManager {
        // 配置过程略...
    }
}
```
按照上面的配置启用 Spring 缓存后，我们即可使用 Spring 自带的缓存注解，如下：
```js
@Service
public class TaskTemplateServiceImpl implements TaskTemplateService {
    
    @Cacheable(cacheNames = CacheNames.TASK_TEMPLATE, key = "#p0")
    public TaskTemplateDTO getTaskTemplate(String taskCode) {
        
    }
    
    @CachePut(cacheNames = CacheNames.TASK_TEMPLATE, key = "#p0.taskCode")
    public void updateTaskTemplate(TaskTemplateDTO template) {
        
    }
    
    @CacheEvict(cacheNames = CacheNames.TASK_TEMPLATE, key = "#p0")
    public void deleteTaskTemplate(String taskCode) {
        
    }
}
```
更多用法请参考 Spring Cache 的相关文档 https://docs.spring.io/spring-framework/docs/3.2.x/spring-framework-reference/html/cache.html




# xi-util
一款打印业务日志的工具
* 支持access、service日志切面打印
* 支持业务日志自定义嵌套打印等

## BizLogger 接口
BizLogger 是一个接口，它长这个样子：
```js
/**
 * 业务日志打印类，在文件中打印出 `key1=value1`key2=value2 的格式，方便在 xflush 中配置监控
 */
public interface BizLogger {

    /**
     * 往日志中添加 key 和 value，返回 this 方便链式调用
     */
    BizLogger ap(String key, Object value);

    /**
     * 往日志中添加 key 和 value，返回 this 方便链式调用
     */
    BizLogger ap(Map<String, Object> values);

    /**
     * 打印该行日志，日志级别为 INFO
     */
    void println();

    /**
     * 打印该行日志，使用指定的日志级别
     */
    void println(LogLevel logLevel);
}
```
这个接口主要有两个方法：ap 方法可以往里面添加 key 和 value，println 方法把这些值拼装成统一的格式，输出到日志文件。For example: 
```js
BizLogger logger = new BizLoggerImpl("logger-name");
try {
    logger.ap("key1", "value1");
    logger.ap("value2", "value2");
} finally {
    logger.println();
}
```
输出结果：
```js
16:52:26.522 [main] INFO logger-name - `key1=value1`value2=value2
```

## ZLog 工具类
单独使用 BizLogger 接口已经可以打印业务日志了，但是如果一个业务逻辑包含了多层方法调用，我们就必须把 logger 对象作为方法参数一层一层传递下去，这样实在是有点丑。

这种情况，我们可以使用 ZLog 工具类，这个类里面有三个静态方法：
* start: 创建一个 BizLogger 对象，存放到当前线程的上下文中
* ap: 往当前上下文中的 logger 对象添加 key 和 value
* end: 调用当前 logger 对象的 println 方法打印日志，并清除线程上下文

使用方法：
```js
ZLog.start("logger-name");
try {
    ZLog.ap("key1", "value1");
    ZLog.ap("value2", "value2");
} finally {
    ZLog.end();
}
```
ZLog 还可以嵌套使用，比如：
```js
private void level1() {
    ZLog.start("level1", true);
    try {
        ZLog.ap("a1", "0").ap("b1", "0");
        level2();
    } finally {
        ZLog.end();
    }
}

private void level2() {
    ZLog.start("level2");
    try {
        ZLog.ap("a2", "0").ap("b2", "0");
        level3();
    } finally {
        ZLog.end();
    }
}

private void level3() {
    ZLog.start("level3");
    try {
        ZLog.ap("a3", "0").ap("b3", "0");
    } finally {
        ZLog.end();
    }
}
```
输出结果：
```js
17:03:16.733 [main] INFO level3 - `a3=0`b3=0
17:03:16.737 [main] INFO level2 - `a2=0`b2=0
17:03:16.737 [main] INFO level1 - `a1=0`b1=0`a2=0`b2=0`a3=0`b3=0
```
注意：最外层的 level1 输出了内层所有变量的值，这是因为我们调用 start 方法时，把第二个参数设置为 true，这个开关可配置是否收集内部嵌套的日志数据，默认为 false

@UseZLog 注解
在方法上加上这个注解，Spring AOP 机制会拦截我们的方法调用，在进入方法之前，使用 ZLog.start 开启上下文，在方法退出之后，使用 ZLog.end 清理上下文。除此之外，这个注解还会自动输出 traceId、当前用户 ID 等更多数据，如果方法发生了异常，还会输出错误信息。

例如：
```js
@UseZLog(logger = "logger-name")
public void test() {
    ZLog.ap("key", "value");
}
```
输出结果：
```js
17:03:16.737 [main] INFO logger-name - `bizType=TestFacadeImpl.test`traceId=0b0af9f416178846664086188e240f`userId=0`key=value`spentTime=1
```
