from dataclasses import dataclass
from math import pi, copysign

from rlbot.utils.game_state_util import *

from rlbottraining.common_exercises.common_base_exercises import StrikerExercise
from rlbottraining.rng import SeededRandomNumberGenerator as SeededRandom
from rlbottraining.common_graders.goal_grader import StrikerGrader, Grader


@dataclass
class RunawayBall(StrikerExercise):

    ball_still: bool = False

    grader: Grader = StrikerGrader(timeout_seconds=6)

    def make_game_state(self, rng: SeededRandom) -> GameState:
        side = copysign(1, rng.n11())
        car_speed = rng.uniform(0, 2200)
        return GameState(
            ball=BallState(physics=Physics(
                location=Vector3(side * rng.uniform(-400, 800), rng.uniform(-1000, 0), 92.75),
                velocity=Vector3(-side * rng.uniform(500, 900), rng.uniform(800, 1300), rng.uniform(0, 400)) if not self.ball_still else Vector3(0, 0, 0),
                angular_velocity=Vector3(0, 0, 0))),
            cars={
                0: CarState(
                    physics=Physics(
                        location=Vector3(side * rng.uniform(500, 1100) * (2 if self.ball_still else 1), car_speed * -1 + rng.uniform(-500, 100) * (2 if self.ball_still else 1), 18),
                        rotation=Rotator(0, pi / 2, 0),
                        velocity=Vector3(0, car_speed, 0) if not self.ball_still else Vector3(0, 0, 0),
                        angular_velocity=Vector3(0, 0, 0)),
                    jumped=False,
                    double_jumped=False,
                    boost_amount=rng.uniform(40, 100))
            },
            boosts={i: BoostState(0) for i in range(34)},
        )
