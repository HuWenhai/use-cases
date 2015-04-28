package my.cool;

import java.util.concurrent.atomic.AtomicLong;

import javax.jms.*;

import junit.framework.TestCase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PriorityAndPrefetch extends TestCase {

    private static final Logger log = LoggerFactory.getLogger(ConsumerThread.class);

    private BrokerService brokerService;

    private final String ACTIVEMQ_BROKER_BIND = "tcp://localhost:0";
    private String ACTIVEMQ_BROKER_URI;

    private ActiveMQConnectionFactory connectionFactory;

    @Override
    protected void setUp() throws Exception {
        // Start an embedded broker up.
        brokerService = new BrokerService();
        brokerService.setPersistent(false);
        brokerService.setUseJmx(false);
        brokerService.setDeleteAllMessagesOnStartup(true);

        brokerService.addConnector(ACTIVEMQ_BROKER_BIND);
        brokerService.start();
        brokerService.waitUntilStarted();
    }

    @Override
    protected void tearDown() throws Exception {
        brokerService.waitUntilStarted();
    }

    public void testTwoConsumersWithPriority1and2() throws Exception {

        int NUM_MESSAGES = 10;
        int EXPECTED_NUM_CONSUMER_HIGH = 10;

        Queue queueHigh = new ActiveMQQueue(getName() + "?consumer.priority=2");
        Queue queueLow = new ActiveMQQueue(getName() + "?consumer.priority=1");
        Queue queue = new ActiveMQQueue(getName());

        ACTIVEMQ_BROKER_URI = brokerService.getTransportConnectors().get(0).getPublishableConnectString();
        Connection connection = new ActiveMQConnectionFactory(ACTIVEMQ_BROKER_URI).createConnection();
        connection.start();

        Session sessionProducer = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session sessionConsumerHigh = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session sessionConsumerLow = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        ProducerThread p1 = new ProducerThread(NUM_MESSAGES, sessionProducer, queue);
        p1.start();
        p1.join();

        ConsumerThread high = new ConsumerThread(EXPECTED_NUM_CONSUMER_HIGH, sessionConsumerHigh, queueHigh);
        high.start();
        high.join();

        ConsumerThread low = new ConsumerThread(0, sessionConsumerLow, queueLow);
        low.start();
        low.join();

        long resultHigh = high.getCounter().addAndGet(0);

        assertEquals(EXPECTED_NUM_CONSUMER_HIGH, resultHigh);

        connection.close();
    }

    public void testTwoConsumersWithPriority1and2AndPrefetchSize5() throws Exception {

        int NUM_MESSAGES = 20;
        int EXPECTED_NUM_CONSUMER_LOW = 15;
        int EXPECTED_NUM_CONSUMER_HIGH = 5;

        Queue queueLow = new ActiveMQQueue(getName() + "?consumer.priority=1&consumer.prefetchSize=100");
        Queue queueHigh = new ActiveMQQueue(getName() + "?consumer.priority=2&consumer.prefetchSize=5");
        Queue queue = new ActiveMQQueue(getName());

        ACTIVEMQ_BROKER_URI = brokerService.getTransportConnectors().get(0).getPublishableConnectString();
        Connection connection = new ActiveMQConnectionFactory(ACTIVEMQ_BROKER_URI).createConnection();
        connection.start();

        Session sessionProducer = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session sessionConsumerHigh = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session sessionConsumerLow = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        ProducerThread p1 = new ProducerThread(NUM_MESSAGES, sessionProducer, queue);
        p1.start();
        p1.join();

        ConsumerThread low = new ConsumerThread(EXPECTED_NUM_CONSUMER_LOW, sessionConsumerLow, queueLow);
        low.start();

        ConsumerThread high = new ConsumerThread(EXPECTED_NUM_CONSUMER_HIGH, sessionConsumerHigh, queueHigh);
        high.start();

        low.join();
        high.join();
        
        long resultLow = low.getCounter().addAndGet(0);
        long resultHigh = high.getCounter().addAndGet(0);

        assertEquals(EXPECTED_NUM_CONSUMER_LOW, resultLow);
        assertEquals(EXPECTED_NUM_CONSUMER_HIGH, resultHigh);

        connection.close();
    }

    public void testTwoConsumersWithOneExclusiveConsumer() throws Exception {

        int NUM_MESSAGES = 10;
        int EXPECTED_NUM_CONSUMER_EXCLUSIVE = 10;

        Queue queueExclusive = new ActiveMQQueue(getName() + "?consumer.exclusive=true");
        Queue queue = new ActiveMQQueue(getName());

        ACTIVEMQ_BROKER_URI = brokerService.getTransportConnectors().get(0).getPublishableConnectString();
        Connection connection = new ActiveMQConnectionFactory(ACTIVEMQ_BROKER_URI).createConnection();
        connection.start();

        Session sessionProducer = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session sessionConsumerExclusive = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session sessionConsumerNonExclusive = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        ProducerThread p1 = new ProducerThread(NUM_MESSAGES, sessionProducer, queue);
        p1.start();
        p1.join();

        ConsumerThread exclusive = new ConsumerThread(EXPECTED_NUM_CONSUMER_EXCLUSIVE, sessionConsumerExclusive, queueExclusive);
        exclusive.start();

        ConsumerThread nonExclusive = new ConsumerThread(0, sessionConsumerNonExclusive, queue);
        nonExclusive.start();
        
        exclusive.join();
        nonExclusive.join();

        long resultHigh = exclusive.getCounter().addAndGet(0);

        assertEquals(EXPECTED_NUM_CONSUMER_EXCLUSIVE, resultHigh);

        connection.close();

    }

    public class ProducerThread extends Thread {

        int NUM_MESSAGES;
        Destination destination;
        Session session;
        MessageProducer producer;

        public ProducerThread(int num_messages, Session session, Destination destination) {
            this.NUM_MESSAGES = num_messages;
            this.session = session;
            this.destination = destination;
        }

        public void run() {
            try {
                producer = session.createProducer(destination);

                for (int i = 0; i < this.NUM_MESSAGES; ++i) {
                    producer.send(session.createTextMessage("TEST"));
                }
            } catch (Exception e) {
                log.error("Caught an unexpected error: ", e);
            } finally {
                try {
                    producer.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class ConsumerThread extends Thread {

        AtomicLong counter = new AtomicLong();
        Destination destination;
        Session session;
        MessageConsumer consumer;
        int NUM_MESSAGES;

        public ConsumerThread(int num_messages, Session session, Destination destination) {
            this.NUM_MESSAGES = num_messages;
            this.session = session;
            this.destination = destination;
        }

        @Override
        public void run() {
            try {
                consumer = session.createConsumer(destination);
                if (NUM_MESSAGES > 0) {
                    while (counter.get() < NUM_MESSAGES) {
                        Message message = consumer.receive(100);
                        counter.incrementAndGet();
                    }
                } else {
                    Message message = consumer.receive(5000); 
                }
            } catch (Exception e) {
                log.error("Caught an unexpected error: ", e);
            } finally {
                try {
                    consumer.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }

        public AtomicLong getCounter() {
            return counter;
        }
    }

}
