from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import GameState, BoostState, BallState, CarState, Physics, Vector3, Rotator
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise
from rlbottraining.rng import SeededRandomNumberGenerator
from rlbottraining.training_exercise import Playlist


@dataclass
class odmuzeiwxqhdifmd(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)

	def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(2490.8398,-3985.97,276.15),
				velocity=Vector3(-609.27094,1042.121,-371.301),
				angular_velocity=Vector3(-1.61381,5.77801,-0.10161),
				rotation=Rotator(-0.38013962,1.32488,0.3973969))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(2425.29,-4639.4097,199.39),
						velocity=Vector3(-797.49097,-249.651,-15.901),
						angular_velocity=Vector3(-3.03761,-4.13571,1.9794099),
						rotation=Rotator(-0.199801,-1.6142272,-2.3683705)),
					jumped=True,
					double_jumped=True,
					boost_amount=100.0),
				1: CarState(
					physics=Physics(
						location=Vector3(2463.5999,-1264.83,17.01),
						velocity=Vector3(-597.831,-1213.0409,0.23099999),
						angular_velocity=Vector3(1.0999999E-4,1.0999999E-4,2.23661),
						rotation=Rotator(-0.00958738,-1.9631119,0.0)),
					jumped=False,
					double_jumped=False,
					boost_amount=100.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
