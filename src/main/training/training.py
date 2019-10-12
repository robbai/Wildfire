from pathlib import Path
import glob

from rlbot.matchconfig.match_config import PlayerConfig, Team
from rlbot.utils.class_importer import import_class_with_base
from rlbottraining.training_exercise import TrainingExercise

from punching_bags import get_opponent


exercise_folder = './exercises/'

# Bots.
wildfire_path = '../python/wildfire.cfg'
wildfire_test_path = '../python/wildfireTest.cfg'
opponent_path = get_opponent()


def get_exercises():
    files = glob.glob(exercise_folder + '*.py')
    classes = [import_class_with_base(file, TrainingExercise).loaded_class for file in files]
    return [exercise_class(exercise_class.__name__) for exercise_class in classes]


def make_default_playlist():
    # Add the exercises
    exercises = get_exercises()

    # Configure Wildfire and friends (or foes!)
    for exercise in exercises:
        exercise.match_config.player_configs = [
            PlayerConfig.bot_config(Path(wildfire_test_path), Team.BLUE),
            PlayerConfig.bot_config(Path(opponent_path), Team.ORANGE)
        ]

    return exercises


if __name__ == '__main__':
    print(get_exercises())
