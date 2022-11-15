package jadex.bdiv3.examples.cleanerworld.cleaner;

import ai.djl.Model;
import ai.djl.ModelException;
import ai.djl.basicdataset.cv.classification.Mnist;
import ai.djl.basicmodelzoo.basic.Mlp;
import ai.djl.engine.Engine;
import ai.djl.engine.EngineProvider;
import ai.djl.examples.training.util.Arguments;
import ai.djl.inference.Predictor;
import ai.djl.metric.Metrics;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.modality.cv.translator.ImageClassificationTranslator;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.pytorch.engine.PtEngineProvider;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingResult;
import ai.djl.training.dataset.Dataset;
import ai.djl.training.dataset.RandomAccessDataset;
import ai.djl.training.evaluator.Accuracy;
import ai.djl.training.listener.SaveModelTrainingListener;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.PlanAPI;
import jadex.bdiv3.annotation.PlanBody;
import jadex.bdiv3.annotation.PlanCapability;
import jadex.bdiv3.runtime.IPlan;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;



// Retrieve an instance of the PyTorch engine


@Plan
public class NeuralNetworkPlan
{

    @PlanCapability
    protected CleanerAgent capa;



    @PlanAPI
    protected IPlan rplan;
    protected Random rnd = new Random();
    private static final Logger logger = LoggerFactory.getLogger(NeuralNetworkPlan.class);

    // added here -oemer95

    public NeuralNetworkPlan() {
       // Classifications classifications = NeuralNetworkPlan.predict();
       // logger.info("{}", classifications);
    }  //changed from private to public oe95

    //public static void main(String[] args) throws IOException, ModelException, TranslateException {

   // }
   @PlanBody
    public IFuture<Void> body() throws ModelException, TranslateException, IOException {



       EngineProvider engineProvider = new PtEngineProvider();
       Engine engine = engineProvider.getEngine();

       final Future<Void> ret = new Future<Void>();
      // Classifications classifications = NeuralNetworkPlan.predict();
       NeuralNetworkPlan.runExample(args);

      // logger.info("{}", classifications);

       // List<Location> loci = capa.getPatrolPoints();
       // moveToLocations(loci.iterator()).addResultListener(new DelegationResultListener<Void>(ret));


       return ret;
   }


   public static Classifications predict() throws IOException, ModelException, TranslateException {

        // C:\Users\Oemer\Desktop\djl-master\djl-master\examples\src\test\resources
       // Path imageFile = Paths.get("C:\\Users\\Oemer\\Desktop\\djl-master\\djl-master\\examples\\src\\test\\resources\\0.png");
       Path imageFile = Paths.get("C:\\Users\\Oemer\\Desktop\\Deep Learning Java Libraries\\djl-0.22.1\\examples\\src\\test\\resources\\0.png");
       // Path imageFile = Paths.get("src/test/resources/0.png");
        Image img = ImageFactory.getInstance().fromFile(imageFile);

        String modelName = "mlp";
        try (Model model = Model.newInstance(modelName)) {
            model.setBlock(new Mlp(28 * 28, 10, new int[] {128, 64}));

            // Assume you have run TrainMnist.java example, and saved model in build/model folder.
            Path modelDir = Paths.get("build/model");
            model.load(modelDir);

            List<String> classes =
                    IntStream.range(0, 10).mapToObj(String::valueOf).collect(Collectors.toList());
            Translator<Image, Classifications> translator =
                    ImageClassificationTranslator.builder()
                            .addTransform(new ToTensor())
                            .optSynset(classes)
                            .optApplySoftmax(true)
                            .build();

            try (Predictor<Image, Classifications> predictor = model.newPredictor(translator)) {
                return predictor.predict(img);
            }
        }
    }

    public static TrainingResult runExample() throws IOException, TranslateException {
        Arguments arguments = new Arguments().parseArgs(args);
        if (arguments == null) {
            return null;
        }

        // Construct neural network
        Block block =
                new Mlp(
                        Mnist.IMAGE_HEIGHT * Mnist.IMAGE_WIDTH,
                        Mnist.NUM_CLASSES,
                        new int[] {128, 64});

        try (Model model = Model.newInstance("mlp")) {
            model.setBlock(block);

            // get training and validation dataset
            RandomAccessDataset trainingSet = getDataset(Dataset.Usage.TRAIN, arguments);
            RandomAccessDataset validateSet = getDataset(Dataset.Usage.TEST, arguments);

            // setup training configuration
            DefaultTrainingConfig config = setupTrainingConfig(arguments);

            try (Trainer trainer = model.newTrainer(config)) {
                trainer.setMetrics(new Metrics());

                /*
                 * MNIST is 28x28 grayscale image and pre processed into 28 * 28 NDArray.
                 * 1st axis is batch axis, we can use 1 for initialization.
                 */
                Shape inputShape = new Shape(1, Mnist.IMAGE_HEIGHT * Mnist.IMAGE_WIDTH);

                // initialize trainer with proper input shape
                trainer.initialize(inputShape);

                EasyTrain.fit(trainer, arguments.getEpoch(), trainingSet, validateSet);

                return trainer.getTrainingResult();
            }
        }
    }

    private static DefaultTrainingConfig setupTrainingConfig(Arguments arguments) {
        String outputDir = arguments.getOutputDir();
        SaveModelTrainingListener listener = new SaveModelTrainingListener(outputDir);
        listener.setSaveModelCallback(
                trainer -> {
                    TrainingResult result = trainer.getTrainingResult();
                    Model model = trainer.getModel();
                    float accuracy = result.getValidateEvaluation("Accuracy");
                    model.setProperty("Accuracy", String.format("%.5f", accuracy));
                    model.setProperty("Loss", String.format("%.5f", result.getValidateLoss()));
                });
        return new DefaultTrainingConfig(Loss.softmaxCrossEntropyLoss())
                .addEvaluator(new Accuracy())
                .optDevices(Engine.getInstance().getDevices(arguments.getMaxGpus()))
                .addTrainingListeners(TrainingListener.Defaults.logging(outputDir))
                .addTrainingListeners(listener);
    }

    private static RandomAccessDataset getDataset(Dataset.Usage usage, Arguments arguments)
            throws IOException {
        Mnist mnist =
                Mnist.builder()
                        .optUsage(usage)
                        .setSampling(arguments.getBatchSize(), true)
                        .optLimit(arguments.getLimit())
                        .build();
        mnist.prepare(new ProgressBar());
        return mnist;
    }





}

