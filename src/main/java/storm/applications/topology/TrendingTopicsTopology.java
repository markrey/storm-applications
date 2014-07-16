package storm.applications.topology;

import backtype.storm.Config;
import backtype.storm.generated.StormTopology;
import backtype.storm.tuple.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import storm.applications.bolt.IntermediateRankingsBolt;
import storm.applications.bolt.RollingCountBolt;
import storm.applications.bolt.TopicExtractorBolt;
import storm.applications.bolt.TotalRankingsBolt;
import storm.applications.constants.TrendingTopicsConstants.Component;
import storm.applications.constants.TrendingTopicsConstants.Conf;
import storm.applications.constants.TrendingTopicsConstants.Field;
import static storm.applications.constants.TrendingTopicsConstants.PREFIX;

public class TrendingTopicsTopology extends BasicTopology {
    private static final Logger LOG = LoggerFactory.getLogger(TrendingTopicsTopology.class);
    
    private int topicExtractorThreads;
    private int counterThreads;
    private int iRankerThreads;
    private int tRankerThreads;
    
    private int topn;
    
    public TrendingTopicsTopology(String topologyName, Config config) {
        super(topologyName, config);
    }

    @Override
    public void initialize() {
        super.initialize();
        
        topicExtractorThreads = config.getInt(Conf.TOPIC_EXTRACTOR_THREADS, 1);
        counterThreads        = config.getInt(Conf.COUNTER_THREADS, 1);
        iRankerThreads        = config.getInt(Conf.IRANKER_THREADS, 1);
        tRankerThreads        = config.getInt(Conf.IRANKER_THREADS, 1);
    }

    @Override
    public StormTopology buildTopology() {
        builder.setSpout(Component.SPOUT, spout, spoutThreads);
        
        builder.setBolt(Component.TOPIC_EXTRACTOR, new TopicExtractorBolt(), topicExtractorThreads)
               .shuffleGrouping(Component.SPOUT);
        
        builder.setBolt(Component.COUNTER, new RollingCountBolt(), counterThreads)
               .fieldsGrouping(Component.TOPIC_EXTRACTOR, new Fields(Field.WORD));
        
        builder.setBolt(Component.INTERMEDIATE_RANKER, new IntermediateRankingsBolt(topn), iRankerThreads)
               .fieldsGrouping(Component.COUNTER, new Fields(Field.OBJ));
        
        builder.setBolt(Component.TOTAL_RANKER, new TotalRankingsBolt(topn), tRankerThreads)
               .globalGrouping(Component.INTERMEDIATE_RANKER);
        
        builder.setBolt(Component.SINK, sink, sinkThreads)
               .shuffleGrouping(Component.TOTAL_RANKER);
        
        return builder.createTopology();
    }

    @Override
    public Logger getLogger() {
        return LOG;
    }

    @Override
    public String getConfigPrefix() {
        return PREFIX;
    }
    
}
