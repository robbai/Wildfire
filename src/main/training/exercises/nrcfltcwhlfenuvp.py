from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import GameState, BoostState, BallState, CarState, Physics, Vector3, Rotator
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise
from rlbottraining.rng import SeededRandomNumberGenerator
from rlbottraining.training_exercise import Playlist


@dataclass
class nrcfltcwhlfenuvp(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)

	def make_game_state(self, rng: SeededRandomNumberGenerator) -> GameState:
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(-2104.1099,5025.9497,164.47),
				velocity=Vector3(299.101,-154.431,253.47101),
				angular_velocity=Vector3(-2.68911,3.7743099,3.09181),
				rotation=Rotator(0.34274882,0.6972901,-0.25080585))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(-2593.05,4884.88,16.69),
						velocity=Vector3(422.621,48.641,6.8209996),
						angular_velocity=Vector3(0.015109999,-0.03071,0.79331),
						rotation=Rotator(-0.01840777,0.086669914,0.0010546118)),
					jumped=False,
					double_jumped=False,
					boost_amount=96.0),
				1: CarState(
					physics=Physics(
						location=Vector3(-2882.72,4746.54,469.69998),
						velocity=Vector3(1246.9609,294.511,-619.78094),
						angular_velocity=Vector3(-4.97911,-2.27091,0.54881),
						rotation=Rotator(-0.5048714,0.48617604,-0.14611167)),
					jumped=True,
					double_jumped=True,
					boost_amount=0.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
